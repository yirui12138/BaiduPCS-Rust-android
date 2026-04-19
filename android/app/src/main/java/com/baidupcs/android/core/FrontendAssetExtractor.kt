// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android.core

import android.content.res.AssetManager
import java.io.File
import java.security.MessageDigest

class FrontendAssetExtractor(
    private val assetManager: AssetManager,
) {
    fun syncIfNeeded(targetDir: File, versionToken: String) {
        val effectiveVersionToken = "$versionToken-${assetFingerprint("www")}"
        val markerFile = File(targetDir, ".asset-version")
        val indexFile = File(targetDir, "index.html")

        if (indexFile.exists() && markerFile.exists() && markerFile.readText() == effectiveVersionToken) {
            return
        }

        targetDir.parentFile?.deleteRecursively()
        copyRecursively("www", targetDir)
        markerFile.writeText(effectiveVersionToken)
    }

    private fun assetFingerprint(assetPath: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        updateDigestRecursively(assetPath, digest)
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun updateDigestRecursively(assetPath: String, digest: MessageDigest) {
        digest.update(assetPath.toByteArray(Charsets.UTF_8))
        val children = assetManager.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            assetManager.open(assetPath).use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return
        }

        children.sorted().forEach { child ->
            updateDigestRecursively("$assetPath/$child", digest)
        }
    }

    private fun copyRecursively(assetPath: String, outputFile: File) {
        val children = assetManager.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            outputFile.parentFile?.mkdirs()
            assetManager.open(assetPath).use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return
        }

        outputFile.mkdirs()
        children.forEach { child ->
            copyRecursively("$assetPath/$child", File(outputFile, child))
        }
    }
}
