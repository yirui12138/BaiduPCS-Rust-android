// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

// 转存模块
//
// 实现分享链接转存 + 可选自动下载功能

pub mod manager;
pub mod task;
pub mod types;

pub use manager::TransferManager;
pub use manager::build_fs_ids;
pub use task::{TransferStatus, TransferTask};
pub use types::{CleanupResult, CleanupStatus, ShareFileListResult, ShareLink, SharePageInfo, SharedFileInfo, TransferError, TransferResult};
