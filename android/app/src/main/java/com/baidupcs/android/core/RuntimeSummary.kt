// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RuntimeSummary(
    val activeDownloads: Int,
    val activeUploads: Int,
    val activeTransfers: Int,
    val activeBackups: Int,
    val hasActiveWork: Boolean,
) {
    companion object {
        val Empty = RuntimeSummary(
            activeDownloads = 0,
            activeUploads = 0,
            activeTransfers = 0,
            activeBackups = 0,
            hasActiveWork = false,
        )
    }
}

object RuntimeSummaryClient {
    private const val CONNECT_TIMEOUT_MS = 1_500
    private const val READ_TIMEOUT_MS = 1_500

    suspend fun fetch(baseUrl: String): RuntimeSummary? = withContext(Dispatchers.IO) {
        val connection = (URL("$baseUrl/api/v1/runtime/summary").openConnection() as HttpURLConnection)

        runCatching {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode !in 200..299) {
                return@runCatching null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val payload = JSONObject(body)
            if (payload.optInt("code", -1) != 0) {
                return@runCatching null
            }

            val data = payload.optJSONObject("data") ?: return@runCatching null
            RuntimeSummary(
                activeDownloads = data.optInt("active_downloads", 0),
                activeUploads = data.optInt("active_uploads", 0),
                activeTransfers = data.optInt("active_transfers", 0),
                activeBackups = data.optInt("active_backups", 0),
                hasActiveWork = data.optBoolean("has_active_work", false),
            )
        }.getOrNull().also {
            connection.disconnect()
        }
    }
}
