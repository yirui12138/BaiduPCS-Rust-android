// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

//! 备份记录模块（去重服务）

pub mod record_manager;

pub use record_manager::{
    BackupRecordManager, UploadRecord, DownloadRecord, EncryptionSnapshot,
    RecordStats, calculate_head_md5,
};
