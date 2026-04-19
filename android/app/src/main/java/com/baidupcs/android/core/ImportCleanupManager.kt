// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android.core

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ImportCleanupResult(
    val deletedCount: Int,
    val failedCount: Int,
)

object ImportCleanupManager {
    private const val TAG = "ImportCleanupManager"
    private const val DEFAULT_STALE_AGE_MS = 7L * 24L * 60L * 60L * 1000L

    suspend fun cleanupImportedPaths(paths: List<String>, uploadRoot: File): ImportCleanupResult =
        withContext(Dispatchers.IO) {
            val root = uploadRoot.canonicalFile
            var deleted = 0
            var failed = 0

            paths
                .mapNotNull { it.takeIf(String::isNotBlank) }
                .distinct()
                .forEach { rawPath ->
                    val target = runCatching { File(rawPath).canonicalFile }.getOrNull()
                    if (target == null || !isChildOf(root, target) || target == root) {
                        failed++
                        Log.w(TAG, "Rejected unsafe import cleanup path: $rawPath")
                        return@forEach
                    }

                    if (!target.exists()) {
                        deleted++
                        return@forEach
                    }

                    val removed = runCatching {
                        if (target.isDirectory) target.deleteRecursively() else target.delete()
                    }.getOrDefault(false)

                    if (removed) {
                        deleted++
                        cleanupEmptyParents(target.parentFile, root)
                    } else {
                        failed++
                        Log.w(TAG, "Failed to delete imported path: ${target.absolutePath}")
                    }
                }

            ImportCleanupResult(deletedCount = deleted, failedCount = failed)
        }

    suspend fun cleanupStaleImports(uploadRoot: File, staleAgeMs: Long = DEFAULT_STALE_AGE_MS): ImportCleanupResult =
        withContext(Dispatchers.IO) {
            val root = uploadRoot.canonicalFile
            val now = System.currentTimeMillis()
            var deleted = 0
            var failed = 0

            root.listFiles().orEmpty().forEach { child ->
                val target = runCatching { child.canonicalFile }.getOrNull() ?: return@forEach
                if (!isChildOf(root, target) || target == root) return@forEach
                if (now - target.lastModified() < staleAgeMs) return@forEach

                val removed = runCatching {
                    if (target.isDirectory) target.deleteRecursively() else target.delete()
                }.getOrDefault(false)

                if (removed) {
                    deleted++
                } else {
                    failed++
                    Log.w(TAG, "Failed to delete stale imported path: ${target.absolutePath}")
                }
            }

            ImportCleanupResult(deletedCount = deleted, failedCount = failed)
        }

    private fun isChildOf(root: File, target: File): Boolean {
        val rootPath = root.absolutePath.trimEnd(File.separatorChar) + File.separator
        return target.absolutePath == root.absolutePath || target.absolutePath.startsWith(rootPath)
    }

    private fun cleanupEmptyParents(start: File?, root: File) {
        var current = start?.canonicalFile ?: return
        while (current != root && isChildOf(root, current)) {
            val children = current.listFiles()
            if (children == null || children.isNotEmpty()) return
            if (!current.delete()) return
            current = current.parentFile?.canonicalFile ?: return
        }
    }
}
