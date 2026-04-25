// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android.nativeui

import com.baidupcs.android.core.FolderOpenResult
import com.baidupcs.android.core.ImportedEntry

data class NativeCookieLoginResult(
    val status: String,
    val cookies: String?,
    val reason: String?,
)

data class NativePlatformActions(
    val importFiles: (onSuccess: (List<ImportedEntry>) -> Unit, onError: (String) -> Unit) -> Unit,
    val importFolder: (onSuccess: (List<ImportedEntry>) -> Unit, onError: (String) -> Unit) -> Unit,
    val openFolder: (path: String) -> FolderOpenResult,
    val cleanupImportedPaths: (List<String>) -> Unit,
    val cleanupStaleImports: () -> Unit,
    val readClipboardText: () -> String,
    val isVpnActive: () -> Boolean,
    val startCookieLogin: (onResult: (NativeCookieLoginResult) -> Unit) -> Unit,
)
