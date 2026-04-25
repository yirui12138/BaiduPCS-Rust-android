// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android.nativeui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserAuth(
    val uid: Long = 0,
    val username: String = "",
    val nickname: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("vip_type")
    val vipType: Int? = null,
    @SerialName("total_space")
    val totalSpace: Long? = null,
    @SerialName("used_space")
    val usedSpace: Long? = null,
    val bduss: String = "",
    @SerialName("login_time")
    val loginTime: Long = 0,
)

@Serializable
data class QrCode(
    val sign: String = "",
    @SerialName("image_base64")
    val imageBase64: String = "",
    @SerialName("qrcode_url")
    val qrcodeUrl: String = "",
    @SerialName("created_at")
    val createdAt: Long = 0,
)

sealed interface QrStatus {
    data object Waiting : QrStatus
    data object Scanned : QrStatus
    data class Success(val user: UserAuth) : QrStatus
    data object Expired : QrStatus
    data class Failed(val reason: String) : QrStatus
}

@Serializable
data class FileItem(
    @SerialName("fs_id")
    val fsId: Long = 0,
    val path: String = "",
    @SerialName("server_filename")
    val serverFilename: String = "",
    val size: Long = 0,
    val isdir: Int = 0,
    val category: Int = 0,
    @SerialName("server_ctime")
    val serverCtime: Long = 0,
    @SerialName("server_mtime")
    val serverMtime: Long = 0,
    @SerialName("is_encrypted")
    val isEncrypted: Boolean = false,
    @SerialName("original_name")
    val originalName: String? = null,
) {
    val displayName: String
        get() = originalName?.takeIf { it.isNotBlank() } ?: serverFilename

    val isDirectory: Boolean
        get() = isdir == 1
}

@Serializable
data class FileListData(
    val list: List<FileItem> = emptyList(),
    val dir: String = "/",
    val page: Int = 1,
    val total: Int = 0,
    @SerialName("has_more")
    val hasMore: Boolean = false,
)

@Serializable
data class DeleteFilesData(
    val success: Boolean = false,
    @SerialName("deleted_count")
    val deletedCount: Int = 0,
    @SerialName("failed_paths")
    val failedPaths: List<String> = emptyList(),
    val error: String? = null,
    val errno: Int? = null,
)

@Serializable
data class RecommendedVipConfig(
    val threads: Int = 1,
    @SerialName("chunk_size")
    val chunkSize: Long = 4,
    @SerialName("max_tasks")
    val maxTasks: Int = 1,
    @SerialName("file_size_limit_gb")
    val fileSizeLimitGb: Long = 4,
)

@Serializable
data class RecommendedConfigResponse(
    @SerialName("vip_type")
    val vipType: Int = 0,
    @SerialName("vip_name")
    val vipName: String = "普通用户",
    val recommended: RecommendedVipConfig = RecommendedVipConfig(),
    val warnings: List<String> = emptyList(),
)

@Serializable
data class BatchDownloadItem(
    @SerialName("fs_id")
    val fsId: Long,
    val path: String,
    val name: String,
    @SerialName("is_dir")
    val isDir: Boolean,
    val size: Long? = null,
    @SerialName("original_name")
    val originalName: String? = null,
)

@Serializable
data class BatchDownloadRequest(
    val items: List<BatchDownloadItem>,
    @SerialName("target_dir")
    val targetDir: String,
    @SerialName("conflict_strategy")
    val conflictStrategy: String? = null,
)

@Serializable
data class BatchDownloadError(
    val path: String = "",
    val reason: String = "",
)

@Serializable
data class BatchDownloadResponse(
    @SerialName("task_ids")
    val taskIds: List<String> = emptyList(),
    @SerialName("folder_task_ids")
    val folderTaskIds: List<String> = emptyList(),
    val failed: List<BatchDownloadError> = emptyList(),
)

@Serializable
data class DownloadTask(
    val id: String = "",
    @SerialName("fs_id")
    val fsId: Long = 0,
    @SerialName("remote_path")
    val remotePath: String = "",
    @SerialName("local_path")
    val localPath: String = "",
    @SerialName("total_size")
    val totalSize: Long = 0,
    @SerialName("downloaded_size")
    val downloadedSize: Long = 0,
    val status: String = "",
    val speed: Long = 0,
    @SerialName("created_at")
    val createdAt: Long = 0,
    val error: String? = null,
)

@Serializable
data class UploadTask(
    val id: String = "",
    @SerialName("local_path")
    val localPath: String = "",
    @SerialName("remote_path")
    val remotePath: String = "",
    @SerialName("total_size")
    val totalSize: Long = 0,
    @SerialName("uploaded_size")
    val uploadedSize: Long = 0,
    val status: String = "",
    val speed: Long = 0,
    @SerialName("created_at")
    val createdAt: Long = 0,
    val error: String? = null,
)

@Serializable
data class TransferTask(
    val id: String = "",
    @SerialName("share_url")
    val shareUrl: String = "",
    @SerialName("save_path")
    val savePath: String = "/",
    @SerialName("auto_download")
    val autoDownload: Boolean = false,
    val status: String = "",
    val error: String? = null,
    @SerialName("transferred_count")
    val transferredCount: Int = 0,
    @SerialName("total_count")
    val totalCount: Int = 0,
    @SerialName("file_name")
    val fileName: String? = null,
    @SerialName("created_at")
    val createdAt: Long = 0,
)

@Serializable
data class TransferListResponse(
    val tasks: List<TransferTask> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class CreateTransferResponse(
    @SerialName("task_id")
    val taskId: String? = null,
    val status: String? = null,
    @SerialName("need_password")
    val needPassword: Boolean = false,
)

@Serializable
data class PreviewShareInfo(
    @SerialName("short_key")
    val shortKey: String = "",
    val shareid: String = "",
    val uk: String = "",
    val bdstoken: String = "",
)

@Serializable
data class SharedFileInfo(
    @SerialName("fs_id")
    val fsId: Long = 0,
    @SerialName("is_dir")
    val isDir: Boolean = false,
    val path: String = "",
    val size: Long = 0,
    val name: String = "",
)

@Serializable
data class PreviewShareResponse(
    val files: List<SharedFileInfo> = emptyList(),
    @SerialName("share_info")
    val shareInfo: PreviewShareInfo? = null,
)

@Serializable
data class ShareRecord(
    val shareId: Long = 0,
    val shortlink: String = "",
    val status: Int = 0,
    val typicalPath: String = "",
    val expiredTime: Long = 0,
    val viewCount: Long = 0,
)

@Serializable
data class ShareResult(
    val link: String = "",
    val pwd: String = "",
    val shareid: Long = 0,
)

@Serializable
data class ShareListData(
    val list: List<ShareRecord> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
)

@Serializable
data class ShareDetailData(
    val pwd: String = "",
    val shorturl: String = "",
)

@Serializable
data class OpenSourceUpstreamInfo(
    val name: String = "",
    val version: String = "",
    val author: String = "",
    val license: String = "",
    @SerialName("releaseUrl")
    val releaseUrl: String = "",
)

@Serializable
data class OpenSourcePackageEntry(
    val source: String = "",
    val name: String = "",
    val version: String = "",
    @SerialName("license_expression")
    val licenseExpression: String = "",
    @SerialName("license_path")
    val licensePath: String = "",
    @SerialName("notice_path")
    val noticePath: String? = null,
    val homepage: String? = null,
)

@Serializable
data class OpenSourceIndex(
    @SerialName("generatedAt")
    val generatedAt: String = "",
    @SerialName("appName")
    val appName: String = "",
    val upstream: OpenSourceUpstreamInfo = OpenSourceUpstreamInfo(),
    val packages: List<OpenSourcePackageEntry> = emptyList(),
)

@Serializable
data class CloudDlTask(
    @SerialName("task_id")
    val taskId: Long = 0,
    val status: Int = 0,
    @SerialName("status_text")
    val statusText: String = "",
    @SerialName("task_name")
    val taskName: String = "",
    @SerialName("source_url")
    val sourceUrl: String = "",
    @SerialName("save_path")
    val savePath: String = "/",
    @SerialName("file_size")
    val fileSize: Long = 0,
    @SerialName("finished_size")
    val finishedSize: Long = 0,
)

@Serializable
data class CloudDlTaskList(
    val tasks: List<CloudDlTask> = emptyList(),
)

@Serializable
data class RuntimeSummary(
    @SerialName("has_active_work")
    val hasActiveWork: Boolean = false,
    @SerialName("active_downloads")
    val activeDownloads: Int = 0,
    @SerialName("active_uploads")
    val activeUploads: Int = 0,
    @SerialName("active_transfers")
    val activeTransfers: Int = 0,
)

data class DetectedShareLink(
    val shareUrl: String,
    val password: String? = null,
)
