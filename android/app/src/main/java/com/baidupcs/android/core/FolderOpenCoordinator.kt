// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android.core

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import java.io.File

data class FolderOpenResult(
    val requestId: String,
    val status: String,
    val path: String,
    val reason: String? = null,
)

private data class FolderIntentCandidate(
    val label: String,
    val intent: Intent,
)

class FolderOpenCoordinator(
    private val context: Context,
) {
    fun openFolder(path: String?, requestId: String): FolderOpenResult {
        if (requestId.isBlank()) {
            return FolderOpenResult(
                requestId = "",
                status = STATUS_FAILED,
                path = path.orEmpty(),
                reason = "missing_request_id",
            )
        }

        val resolvedFolder = resolveFolder(path)
            ?: return failure(requestId, path.orEmpty(), "invalid_path")

        if (!resolvedFolder.exists()) {
            return failure(requestId, resolvedFolder.absolutePath, "directory_not_found")
        }

        if (!resolvedFolder.isDirectory) {
            return failure(requestId, resolvedFolder.absolutePath, "not_a_directory")
        }

        val candidates = buildCandidatesInternal(resolvedFolder)
        if (candidates.isEmpty()) {
            return failure(requestId, resolvedFolder.absolutePath, "no_supported_intent")
        }

        for (candidate in candidates) {
            val launchIntent = candidate.intent.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (launchIntent.resolveActivity(context.packageManager) == null) {
                Log.i(
                    TAG,
                    "Skipping folder open candidate without handler: ${candidate.label}, path=${resolvedFolder.absolutePath}",
                )
                continue
            }

            try {
                Log.i(
                    TAG,
                    "Launching folder open candidate: ${candidate.label}, path=${resolvedFolder.absolutePath}",
                )
                context.startActivity(launchIntent)
                return FolderOpenResult(
                    requestId = requestId,
                    status = STATUS_OPENED,
                    path = resolvedFolder.absolutePath,
                )
            } catch (error: ActivityNotFoundException) {
                Log.w(
                    TAG,
                    "Folder open candidate failed without matching activity: ${candidate.label}, path=${resolvedFolder.absolutePath}",
                    error,
                )
            } catch (error: SecurityException) {
                Log.w(
                    TAG,
                    "Folder open candidate denied by system: ${candidate.label}, path=${resolvedFolder.absolutePath}",
                    error,
                )
            } catch (error: IllegalArgumentException) {
                Log.w(
                    TAG,
                    "Folder open candidate rejected its arguments: ${candidate.label}, path=${resolvedFolder.absolutePath}",
                    error,
                )
            } catch (error: RuntimeException) {
                Log.w(
                    TAG,
                    "Folder open candidate crashed while launching: ${candidate.label}, path=${resolvedFolder.absolutePath}",
                    error,
                )
            }
        }

        return failure(requestId, resolvedFolder.absolutePath, "launch_failed")
    }

    internal fun resolveFolder(path: String?): File? {
        if (path.isNullOrBlank()) return null

        val target = File(path.trim())
        if (target.exists()) {
            return if (target.isDirectory) target else target.parentFile
        }

        if (target.extension.isNotBlank()) {
            return target.parentFile
        }

        return target
    }

    internal fun buildCandidates(folder: File): List<Intent> =
        buildCandidatesInternal(folder).map { it.intent }

    private fun buildCandidatesInternal(folder: File): List<FolderIntentCandidate> =
        buildList {
            for (packageName in DOCUMENTS_UI_PACKAGES) {
                buildDirectoryViewIntent(folder, packageName)?.let { intent ->
                    add(FolderIntentCandidate(label = "document_view_$packageName", intent = intent))
                }
            }
            buildDirectoryViewIntent(folder, packageName = null)?.let { intent ->
                add(FolderIntentCandidate(label = "document_view_generic", intent = intent))
            }
            buildDirectoryTreeIntent(folder)?.let { intent ->
                add(FolderIntentCandidate(label = "document_tree", intent = intent))
            }
            buildDownloadsFallbackIntent(folder)?.let { intent ->
                add(FolderIntentCandidate(label = "downloads_fallback", intent = intent))
            }
            add(FolderIntentCandidate(label = "legacy_folder_view", intent = buildLegacyFolderViewIntent(folder)))
        }

    private fun buildDirectoryViewIntent(folder: File, packageName: String?): Intent? {
        val documentUri = buildExternalStorageDocumentUri(folder) ?: return null
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(documentUri, DocumentsContract.Document.MIME_TYPE_DIR)
            packageName?.let(::setPackage)
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            putExtra("android.provider.extra.SHOW_ADVANCED", true)
            putExtra("android.content.extra.SHOW_ADVANCED", true)
        }
    }

    private fun buildDirectoryTreeIntent(folder: File): Intent? {
        val treeUri = buildExternalStorageTreeUri(folder) ?: return null
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, treeUri)
            putExtra("android.provider.extra.SHOW_ADVANCED", true)
            putExtra("android.content.extra.SHOW_ADVANCED", true)
        }
    }

    private fun buildDownloadsFallbackIntent(folder: File): Intent? {
        val downloadRoot = publicDownloadsRoot() ?: return null
        if (!folder.absolutePath.startsWith(downloadRoot.absolutePath, ignoreCase = true)) {
            return null
        }
        return Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
    }

    private fun buildLegacyFolderViewIntent(folder: File): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.fromFile(folder), "resource/folder")
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun buildExternalStorageDocumentUri(folder: File): Uri? {
        val docId = buildPrimaryStorageDocId(folder) ?: return null
        return DocumentsContract.buildDocumentUri(
            EXTERNAL_STORAGE_AUTHORITY,
            docId,
        )
    }

    private fun buildExternalStorageTreeUri(folder: File): Uri? {
        val docId = buildPrimaryStorageDocId(folder) ?: return null
        return DocumentsContract.buildTreeDocumentUri(
            EXTERNAL_STORAGE_AUTHORITY,
            docId,
        )
    }

    @Suppress("DEPRECATION")
    private fun publicDownloadsRoot(): File? =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    @Suppress("DEPRECATION")
    private fun buildPrimaryStorageDocId(folder: File): String? {
        val externalRoot = Environment.getExternalStorageDirectory() ?: return null
        val externalPath = externalRoot.absolutePath
        val folderPath = folder.absolutePath
        if (!folderPath.startsWith(externalPath, ignoreCase = true)) return null

        val relativePath = folderPath.removePrefix(externalPath).trimStart(File.separatorChar)
        if (relativePath.isBlank()) return null

        return "primary:${relativePath.replace(File.separatorChar, '/')}"
    }

    private fun failure(requestId: String, path: String, reason: String): FolderOpenResult {
        Log.w(TAG, "Folder open failed: requestId=$requestId, path=$path, reason=$reason")
        return FolderOpenResult(
            requestId = requestId,
            status = STATUS_FAILED,
            path = path,
            reason = reason,
        )
    }

    companion object {
        const val STATUS_OPENED = "opened"
        const val STATUS_FAILED = "failed"

        private const val TAG = "FolderOpenCoordinator"
        private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
        private val DOCUMENTS_UI_PACKAGES = listOf(
            "com.google.android.documentsui",
            "com.android.documentsui",
        )
    }
}
