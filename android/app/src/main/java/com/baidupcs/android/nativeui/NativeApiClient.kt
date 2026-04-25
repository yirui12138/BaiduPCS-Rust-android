// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android.nativeui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class NativeApiException(
    val code: Int,
    override val message: String,
) : IOException(message)

class NativeApiClient(
    baseUrl: String,
    private val client: OkHttpClient = defaultHttpClient,
) {
    companion object {
        private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        @OptIn(ExperimentalSerializationApi::class)
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }
        private val defaultHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val apiBase = baseUrl.trimEnd('/') + "/api/v1"

    suspend fun currentUser(): UserAuth = get("/auth/user", UserAuth.serializer())

    suspend fun generateQrCode(): QrCode = post("/auth/qrcode/generate", null, QrCode.serializer())

    suspend fun qrStatus(sign: String): QrStatus {
        val data = getElement("/auth/qrcode/status", mapOf("sign" to sign))
        val obj = data.jsonObject
        return when (obj["status"]?.jsonPrimitive?.contentOrNull) {
            "waiting" -> QrStatus.Waiting
            "scanned" -> QrStatus.Scanned
            "success" -> QrStatus.Success(json.decodeFromJsonElement(UserAuth.serializer(), obj["user"] ?: JsonObject(emptyMap())))
            "expired" -> QrStatus.Expired
            "failed" -> QrStatus.Failed(obj["reason"]?.jsonPrimitive?.contentOrNull ?: "登录失败")
            else -> QrStatus.Waiting
        }
    }

    suspend fun loginWithCookies(cookies: String): UserAuth =
        post(
            path = "/auth/cookie/login",
            body = buildJsonObject { put("cookies", cookies) },
            serializer = UserAuth.serializer(),
        )

    suspend fun logout() {
        postElement("/auth/logout")
    }

    suspend fun files(dir: String, page: Int = 1, pageSize: Int = 50): FileListData =
        get(
            path = "/files",
            params = mapOf("dir" to dir, "page" to page.toString(), "page_size" to pageSize.toString()),
            serializer = FileListData.serializer(),
        )

    suspend fun createFolder(path: String) {
        postElement("/files/folder", buildJsonObject { put("path", path) })
    }

    suspend fun deleteFiles(paths: List<String>): DeleteFilesData =
        post(
            path = "/files/delete",
            body = buildJsonObject { put("paths", kotlinx.serialization.json.JsonArray(paths.map(::JsonPrimitive))) },
            serializer = DeleteFilesData.serializer(),
        )

    suspend fun recommendedConfig(): RecommendedConfigResponse =
        get("/config/recommended", RecommendedConfigResponse.serializer())

    suspend fun createBatchDownload(request: BatchDownloadRequest): BatchDownloadResponse =
        post(
            path = "/downloads/batch",
            body = buildJsonObject {
                put(
                    "items",
                    JsonArray(
                        request.items.map { item ->
                            buildJsonObject {
                                put("fs_id", item.fsId)
                                put("path", item.path)
                                put("name", item.name)
                                put("is_dir", item.isDir)
                                put("size", item.size ?: 0L)
                                item.originalName?.takeIf { it.isNotBlank() }?.let { put("original_name", it) }
                            }
                        },
                    ),
                )
                put("target_dir", request.targetDir)
                request.conflictStrategy?.takeIf { it.isNotBlank() }?.let { put("conflict_strategy", it) }
            },
            serializer = BatchDownloadResponse.serializer(),
        )

    suspend fun createFileDownload(file: FileItem): String =
        postString(
            "/downloads",
            buildJsonObject {
                put("fs_id", file.fsId)
                put("remote_path", file.path)
                put("filename", file.displayName)
                put("total_size", file.size)
                put("conflict_strategy", "auto_rename")
            },
        )

    suspend fun createFolderDownload(file: FileItem): String =
        postString(
            "/downloads/folder",
            buildJsonObject {
                put("path", file.path)
                put("original_name", file.displayName)
                put("conflict_strategy", "auto_rename")
            },
        )

    suspend fun downloads(): List<DownloadTask> =
        get("/downloads", ListSerializer(DownloadTask.serializer()))

    suspend fun pauseDownload(id: String) {
        postElement("/downloads/$id/pause")
    }

    suspend fun resumeDownload(id: String) {
        postElement("/downloads/$id/resume")
    }

    suspend fun deleteDownload(id: String, deleteFile: Boolean = false) {
        deleteElement("/downloads/$id", mapOf("delete_file" to deleteFile.toString()))
    }

    suspend fun uploads(): List<UploadTask> =
        get("/uploads", ListSerializer(UploadTask.serializer()))

    suspend fun createUpload(localPath: String, remotePath: String, encrypt: Boolean): String =
        postString(
            "/uploads",
            buildJsonObject {
                put("local_path", localPath)
                put("remote_path", remotePath)
                put("encrypt", encrypt)
                put("conflict_strategy", "auto_rename")
            },
        )

    suspend fun createFolderUpload(localFolder: String, remoteFolder: String, encrypt: Boolean): List<String> =
        post(
            "/uploads/folder",
            buildJsonObject {
                put("local_folder", localFolder)
                put("remote_folder", remoteFolder)
                put("encrypt", encrypt)
                put("conflict_strategy", "auto_rename")
            },
            ListSerializer(String.serializer()),
        )

    suspend fun pauseUpload(id: String) {
        postElement("/uploads/$id/pause")
    }

    suspend fun resumeUpload(id: String) {
        postElement("/uploads/$id/resume")
    }

    suspend fun deleteUpload(id: String) {
        deleteElement("/uploads/$id")
    }

    suspend fun transfers(): TransferListResponse =
        get("/transfers", TransferListResponse.serializer())

    suspend fun createTransfer(
        shareUrl: String,
        password: String?,
        savePath: String,
        autoDownload: Boolean,
        selectedFsIds: List<Long>? = null,
    ): CreateTransferResponse =
        post(
            "/transfers",
            buildJsonObject {
                put("share_url", shareUrl)
                password?.takeIf { it.isNotBlank() }?.let { put("password", it) }
                put("save_path", savePath.ifBlank { "/" })
                put("save_fs_id", 0)
                put("auto_download", autoDownload)
                selectedFsIds?.takeIf { it.isNotEmpty() }?.let { ids ->
                    put("selected_fs_ids", JsonArray(ids.map { JsonPrimitive(it) }))
                }
            },
            CreateTransferResponse.serializer(),
        )

    suspend fun previewShareFiles(shareUrl: String, password: String?): PreviewShareResponse =
        post(
            "/transfers/preview",
            buildJsonObject {
                put("share_url", shareUrl)
                password?.takeIf { it.isNotBlank() }?.let { put("password", it) }
                put("page", 1)
                put("num", 100)
            },
            PreviewShareResponse.serializer(),
        )

    suspend fun cancelTransfer(id: String) {
        postElement("/transfers/$id/cancel")
    }

    suspend fun deleteTransfer(id: String) {
        deleteElement("/transfers/$id")
    }

    suspend fun shares(page: Int = 1): ShareListData =
        get("/shares", mapOf("page" to page.toString()), ShareListData.serializer())

    suspend fun shareDetail(shareId: Long): ShareDetailData =
        get("/shares/$shareId", ShareDetailData.serializer())

    suspend fun createShare(paths: List<String>, period: Int = 7, pwd: String? = null): ShareResult =
        post(
            "/shares",
            buildJsonObject {
                put("paths", JsonArray(paths.map { JsonPrimitive(it) }))
                put("period", period)
                pwd?.takeIf { it.isNotBlank() }?.let { put("pwd", it) }
            },
            ShareResult.serializer(),
        )

    suspend fun cancelShares(ids: List<Long>) {
        postElement(
            "/shares/cancel",
            buildJsonObject { put("share_ids", JsonArray(ids.map(::JsonPrimitive))) },
        )
    }

    suspend fun cloudTasks(): CloudDlTaskList =
        get("/cloud-dl/tasks", CloudDlTaskList.serializer())

    suspend fun addCloudTask(sourceUrl: String, savePath: String, autoDownload: Boolean) {
        postElement(
            "/cloud-dl/tasks",
            buildJsonObject {
                put("source_url", sourceUrl)
                put("save_path", savePath.ifBlank { "/" })
                put("auto_download", autoDownload)
            },
        )
    }

    suspend fun cancelCloudTask(id: Long) {
        postElement("/cloud-dl/tasks/$id/cancel")
    }

    suspend fun deleteCloudTask(id: Long) {
        deleteElement("/cloud-dl/tasks/$id")
    }

    suspend fun config(): JsonObject = getElement("/config").jsonObject

    suspend fun updateConfig(config: JsonObject) {
        putElement("/config", config)
    }

    suspend fun updateRecentDir(dirType: String, path: String) {
        postElement(
            "/config/recent-dir",
            buildJsonObject {
                put("dir_type", dirType)
                put("path", path)
            },
        )
    }

    suspend fun setDefaultDownloadDir(path: String) {
        postElement("/config/default-download-dir", buildJsonObject { put("path", path) })
    }

    suspend fun resetConfig() {
        postElement("/config/reset")
    }

    suspend fun transferConfig(): JsonObject = getElement("/config/transfer").jsonObject

    suspend fun updateTransferConfig(body: JsonObject) {
        putElement("/config/transfer", body)
    }

    suspend fun encryptionStatus(): JsonObject = getElement("/encryption/status").jsonObject

    suspend fun generateEncryptionKey(algorithm: String): String =
        postString("/encryption/key/generate", buildJsonObject { put("algorithm", algorithm) })

    suspend fun importEncryptionKey(key: String, algorithm: String) {
        postElement(
            "/encryption/key/import",
            buildJsonObject {
                put("key", key)
                put("algorithm", algorithm)
            },
        )
    }

    suspend fun exportEncryptionKey(): String =
        getElement("/encryption/key/export").jsonPrimitive.contentOrNull.orEmpty()

    suspend fun deleteEncryptionKey() {
        deleteElement("/encryption/key")
    }

    suspend fun updateClipboardDetection(enabled: Boolean) {
        updateMobileFlag("clipboard_share_detection_enabled", enabled)
    }

    suspend fun updateVpnWarning(enabled: Boolean) {
        updateMobileFlag("vpn_warning_enabled", enabled)
    }

    private suspend fun updateMobileFlag(key: String, enabled: Boolean) {
        val current = config()
        val mobile = current["mobile"]?.jsonObject ?: JsonObject(emptyMap())
        val nextMobile = JsonObject(mobile + (key to JsonPrimitive(enabled)))
        putElement("/config", JsonObject(current + ("mobile" to nextMobile)))
    }

    suspend fun runtimeSummary(): RuntimeSummary =
        get("/runtime/summary", RuntimeSummary.serializer())

    suspend fun getElement(path: String, params: Map<String, String> = emptyMap()): JsonElement =
        request("GET", path, params, null)

    private suspend fun postElement(path: String, body: JsonElement? = null): JsonElement =
        request("POST", path, emptyMap(), body)

    private suspend fun putElement(path: String, body: JsonElement): JsonElement =
        request("PUT", path, emptyMap(), body)

    private suspend fun deleteElement(path: String, params: Map<String, String> = emptyMap()): JsonElement =
        request("DELETE", path, params, null)

    private suspend fun postString(path: String, body: JsonElement): String {
        val data = postElement(path, body)
        return data.jsonPrimitive.contentOrNull ?: data.toString()
    }

    private suspend fun <T> get(
        path: String,
        serializer: KSerializer<T>,
    ): T = json.decodeFromJsonElement(serializer, getElement(path))

    private suspend fun <T> get(
        path: String,
        params: Map<String, String>,
        serializer: KSerializer<T>,
    ): T = json.decodeFromJsonElement(serializer, getElement(path, params))

    private suspend fun <T> post(
        path: String,
        body: JsonElement?,
        serializer: KSerializer<T>,
    ): T = json.decodeFromJsonElement(serializer, postElement(path, body))

    private suspend fun request(
        method: String,
        path: String,
        params: Map<String, String>,
        body: JsonElement?,
    ): JsonElement = withContext(Dispatchers.IO) {
        val urlBuilder = (apiBase + path).toHttpUrl().newBuilder()
        params.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value) }

        val requestBody = body?.let { json.encodeToString(JsonElement.serializer(), it).toRequestBody(jsonMediaType) }
        val request = Request.Builder()
            .url(urlBuilder.build())
            .method(method, if (method == "GET" || method == "DELETE") null else requestBody ?: ByteArray(0).toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw NativeApiException(response.code, "请求失败 (${response.code})")
            }
            val root = runCatching { json.parseToJsonElement(text).jsonObject }
                .getOrElse { throw NativeApiException(-1, "响应解析失败") }
            val code = root["code"]?.jsonPrimitive?.intOrNull ?: 0
            val message = root["message"]?.jsonPrimitive?.contentOrNull ?: "请求失败"
            if (code != 0) {
                throw NativeApiException(code, message)
            }
            root["data"] ?: JsonNull
        }
    }
}

fun parseShareLink(text: String): DetectedShareLink? {
    if (text.isBlank()) return null
    val urlMatch = Regex("""https?://pan\.baidu\.com/[^\s，。；,;]+""").find(text) ?: return null
    val url = urlMatch.value.trim()
    val pwd = Regex("""(?:pwd=|提取码[:：\s]*|密码[:：\s]*)([A-Za-z0-9]{4})""")
        .find(text)
        ?.groupValues
        ?.getOrNull(1)
    return DetectedShareLink(url, pwd)
}

fun parseBaiduShareLink(text: String): DetectedShareLink? {
    if (text.isBlank()) return null
    val urlMatch = Regex(
        pattern = """(?:https?://)?(?:pan\.baidu\.com|yun\.baidu\.com)/(?:s/[A-Za-z0-9_-]+|share/init\?surl=[A-Za-z0-9_-]+)[^\s，。；;]*""",
        option = RegexOption.IGNORE_CASE,
    ).find(text) ?: return null
    val rawUrl = urlMatch.value.trim().trimEnd(',', ';', '，', '。', '；')
    val url = if (rawUrl.startsWith("http", ignoreCase = true)) rawUrl else "https://$rawUrl"
    val password = listOf(
        Regex("""[?&]pwd=([A-Za-z0-9]{4})""", RegexOption.IGNORE_CASE),
        Regex("""(?:提取码|访问码|密码|pwd|code)[：:\s=]+([A-Za-z0-9]{4})""", RegexOption.IGNORE_CASE),
    ).firstNotNullOfOrNull { pattern ->
        pattern.find(text)?.groupValues?.getOrNull(1)
    }
    return DetectedShareLink(url, password)
}

fun formatBytes(value: Long): String {
    if (value <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var size = value.toDouble()
    var index = 0
    while (size >= 1024 && index < units.lastIndex) {
        size /= 1024
        index += 1
    }
    return if (index == 0) {
        "${size.toLong()} ${units[index]}"
    } else {
        "%.1f %s".format(size, units[index])
    }
}

fun progress(done: Long, total: Long): Float =
    if (total <= 0) 0f else (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
