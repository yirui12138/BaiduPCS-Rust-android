// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

// 网盘API模块

pub mod client;
pub mod cloud_dl;
pub mod cloud_dl_monitor;
pub mod types;

pub use client::NetdiskClient;
pub use cloud_dl::{
    AddTaskRequest, AddTaskResponse, AutoDownloadConfig, ClearTasksResponse, CloudDlFileInfo,
    CloudDlTaskInfo, CloudDlTaskStatus, ListTaskRequest, OperationResponse, QueryTaskRequest,
    TaskListResponse,
};
pub use cloud_dl_monitor::{CloudDlEvent, CloudDlMonitor, PollingConfig, TaskProgressTracker};
pub use types::*;

// TODO: 后续实现
// pub mod file;
