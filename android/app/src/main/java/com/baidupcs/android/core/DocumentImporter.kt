// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android.core

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ImportedEntry(
    val name: String,
    val path: String,
    val entryType: String,
)

object DocumentImporter {
    suspend fun importDocuments(
        context: Context,
        uris: List<Uri>,
        targetDir: File,
    ): List<ImportedEntry> = withContext(Dispatchers.IO) {
        targetDir.mkdirs()
        buildList {
            uris.forEach { uri ->
            val name = DocumentFile.fromSingleUri(context, uri)?.name ?: "imported-file"
                val targetFile = uniqueFile(targetDir, name)
                copyUriToFile(context, uri, targetFile)
                add(
                    ImportedEntry(
                        name = targetFile.name,
                        path = targetFile.absolutePath,
                        entryType = "file",
                    ),
                )
            }
        }
    }

    suspend fun importTree(
        context: Context,
        treeUri: Uri,
        targetDir: File,
    ): List<ImportedEntry> = withContext(Dispatchers.IO) {
        targetDir.mkdirs()
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val targetRoot = uniqueDirectory(targetDir, root.name ?: "imported-folder")
        copyDocumentFile(
            context = context,
            document = root,
            target = targetRoot,
        )
        listOf(
            ImportedEntry(
                name = targetRoot.name,
                path = targetRoot.absolutePath,
                entryType = "directory",
            ),
        )
    }

    private fun copyDocumentFile(
        context: Context,
        document: DocumentFile,
        target: File,
    ): Int {
        if (document.isDirectory) {
            target.mkdirs()
            var imported = 0
            document.listFiles().forEach { child ->
                val childTarget = if (child.isDirectory) {
                    uniqueDirectory(target, child.name ?: "folder")
                } else {
                    uniqueFile(target, child.name ?: "file")
                }
                imported += copyDocumentFile(context, child, childTarget)
            }
            return imported
        }

        copyUriToFile(context, document.uri, target)
        return 1
    }

    private fun copyUriToFile(
        context: Context,
        uri: Uri,
        target: File,
    ) {
        target.parentFile?.mkdirs()
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: error("Unable to open $uri")
    }

    private fun uniqueFile(parent: File, preferredName: String): File {
        val sanitized = preferredName.ifBlank { "file" }
        val dotIndex = sanitized.lastIndexOf('.')
        val baseName = if (dotIndex > 0) sanitized.substring(0, dotIndex) else sanitized
        val extension = if (dotIndex > 0) sanitized.substring(dotIndex) else ""

        var candidate = File(parent, sanitized)
        var counter = 1
        while (candidate.exists()) {
            candidate = File(parent, "$baseName-$counter$extension")
            counter += 1
        }
        return candidate
    }

    private fun uniqueDirectory(parent: File, preferredName: String): File {
        val sanitized = preferredName.ifBlank { "folder" }
        var candidate = File(parent, sanitized)
        var counter = 1
        while (candidate.exists()) {
            candidate = File(parent, "$sanitized-$counter")
            counter += 1
        }
        return candidate
    }
}
