// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    baidu_netdisk_rust::runtime::run_until_shutdown(async {
        let _ = tokio::signal::ctrl_c().await;
    })
    .await
}
