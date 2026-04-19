// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android.core

import android.content.Context
import android.os.Environment
import com.baidupcs.android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class ServerEnvironment(
    val baseUrl: String,
    val downloadDir: File,
    val uploadDir: File,
)

class ServerBootstrapper(
    private val context: Context,
) {
    companion object {
        private const val SERVER_PORT = 18888
        private const val HEALTH_TIMEOUT_MS = 30_000L
        private const val HEALTH_RETRY_DELAY_MS = 500L
        private const val CONNECT_TIMEOUT_MS = 1_500

        private val bootstrapMutex = Mutex()

        @Volatile
        private var cachedEnvironment: ServerEnvironment? = null
    }

    suspend fun ensureStarted(
        forceRestart: Boolean,
        onStatus: (String) -> Unit,
    ): Result<ServerEnvironment> = withContext(Dispatchers.IO) {
        bootstrapMutex.withLock {
            runCatching {
                if (forceRestart) {
                    onStatus("正在重启本地服务")
                    NativeBridge.stopServer()
                    cachedEnvironment = null
                } else {
                    cachedEnvironment?.takeIf { environment ->
                        isHealthy() &&
                            ensureDirectoryWritable(environment.downloadDir) &&
                            ensureDirectoryWritable(environment.uploadDir)
                    }?.let { environment ->
                        onStatus("复用本地运行环境")
                        return@runCatching environment
                    }
                }

                val homeDir = File(context.filesDir, "baidupcs-runtime")
                val downloadDir = resolveDownloadDir(homeDir, onStatus)
                val uploadDir = (
                    context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                        ?: File(homeDir, "upload-space")
                    ).resolve("UploadSpace")
                val frontendDir = File(homeDir, "frontend/dist")

                homeDir.mkdirs()
                uploadDir.mkdirs()

                onStatus("同步前端资源")
                FrontendAssetExtractor(context.assets).syncIfNeeded(
                    targetDir = frontendDir,
                    versionToken = "${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE}",
                )

                val environment = ServerEnvironment(
                    baseUrl = "http://127.0.0.1:$SERVER_PORT",
                    downloadDir = downloadDir,
                    uploadDir = uploadDir,
                )

                if (!forceRestart && isHealthy()) {
                    onStatus("本地服务已就绪")
                    cachedEnvironment = environment
                    return@runCatching environment
                }

                onStatus("启动本地 Rust 核心")
                val started = NativeBridge.startServer(
                    homeDir = homeDir.absolutePath,
                    downloadDir = downloadDir.absolutePath,
                    uploadDir = uploadDir.absolutePath,
                    port = SERVER_PORT,
                )

                check(started) {
                    NativeBridge.getLastError().ifBlank { "native server start failed" }
                }

                onStatus("等待服务就绪")
                waitUntilHealthy()

                cachedEnvironment = environment
                environment
            }.onFailure {
                if (forceRestart) {
                    cachedEnvironment = null
                }
            }
        }
    }

    private suspend fun waitUntilHealthy() {
        withTimeout(HEALTH_TIMEOUT_MS) {
            while (true) {
                if (isHealthy()) {
                    return@withTimeout
                }
                delay(HEALTH_RETRY_DELAY_MS)
            }
        }
    }

    private fun isHealthy(): Boolean {
        val connection = (URL("http://127.0.0.1:$SERVER_PORT/health").openConnection() as HttpURLConnection)
        return runCatching {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = CONNECT_TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.connect()
            connection.responseCode in 200..299
        }.getOrDefault(false).also {
            connection.disconnect()
        }
    }

    private fun resolveDownloadDir(
        homeDir: File,
        onStatus: (String) -> Unit,
    ): File {
        val publicDownloadDir = publicDownloadDir()
        if (publicDownloadDir != null && ensureDirectoryWritable(publicDownloadDir)) {
            onStatus("使用公共 Download 目录")
            return publicDownloadDir
        }

        if (publicDownloadDir != null) {
            onStatus("公共 Download 不可写，回退应用目录")
        }

        val fallbackDir = (
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(homeDir, "downloads")
            ).resolve("BaiduPCS")
        ensureDirectoryWritable(fallbackDir)
        return fallbackDir
    }

    @Suppress("DEPRECATION")
    private fun publicDownloadDir(): File? {
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) ?: return null
        return root.resolve("BaiduPCS")
    }

    private fun ensureDirectoryWritable(dir: File): Boolean = runCatching {
        if (!dir.exists() && !dir.mkdirs()) {
            return@runCatching false
        }
        val probe = File(dir, ".baidupcs_probe_${System.nanoTime()}")
        probe.writeText("ok")
        probe.delete()
        true
    }.getOrDefault(false)
}
