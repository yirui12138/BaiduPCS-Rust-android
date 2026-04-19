// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

// API处理器模块

pub mod auth;
pub mod autobackup;
pub mod cloud_dl;
pub mod common;
pub mod config;
pub mod download;
pub mod encryption_export;
pub mod file;
pub mod filesystem;
pub mod folder_download;
pub mod runtime_status;
pub mod share;
pub mod transfer;
pub mod upload;

pub use auth::*;
pub use config::*;
pub use download::*;
pub use encryption_export::{export_bundle, export_keys, export_mapping};
pub use file::*;
// 只导出需要的函数，避免 ApiResponse 冲突
pub use filesystem::{get_roots, goto_path, list_directory, validate_path};
pub use folder_download::*;
pub use runtime_status::*;
pub use share::*;
pub use transfer::*;
pub use upload::*;
