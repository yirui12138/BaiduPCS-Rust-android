// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

// 本地文件系统浏览模块
//
// 提供模拟操作系统文件资源管理器的能力，用于上传文件选择

mod guard;
mod service;
mod types;

pub use guard::PathGuard;
pub use service::FilesystemService;
pub use types::*;
