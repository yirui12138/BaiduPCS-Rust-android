// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

package com.baidupcs.android.core

import androidx.annotation.Keep

object NativeBridge {
    init {
        System.loadLibrary("baidu_netdisk_rust")
    }

    @Keep
    @JvmStatic
    external fun startServer(
        homeDir: String,
        downloadDir: String,
        uploadDir: String,
        port: Int,
    ): Boolean

    @Keep
    @JvmStatic
    external fun stopServer(): Boolean

    @Keep
    @JvmStatic
    external fun getLastError(): String
}
