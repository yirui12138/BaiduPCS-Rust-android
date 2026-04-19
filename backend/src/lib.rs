// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

pub mod auth;
pub mod autobackup;
pub mod common;
pub mod config;
pub mod downloader;
pub mod encryption;
pub mod filesystem;
pub mod logging;
pub mod netdisk;
pub mod persistence;
pub mod runtime;
pub mod server;
pub mod sign;
pub mod task_slot_pool;
pub mod transfer;
pub mod uploader;
pub mod web_auth;

#[cfg(target_os = "android")]
mod android;

pub use auth::{LoginRequest, LoginResponse, QRCode, QRCodeStatus, UserAuth};
pub use common::{
    ProxyConfig, ProxyType, RefreshCoordinator, RefreshCoordinatorConfig, SpeedAnomalyConfig,
    SpeedAnomalyDetector, StagnationConfig, ThreadStagnationDetector,
};
pub use config::{AppConfig, PersistenceConfig};
pub use downloader::{DownloadManager, DownloadTask, TaskStatus};
pub use netdisk::{FileItem, NetdiskClient};
pub use persistence::{TaskMetadata, TaskPersistenceInfo, TaskType, WalRecord};
pub use server::AppState;
pub use sign::{generate_devuid, LocateSign};
pub use task_slot_pool::{
    SlotTouchThrottler, TaskPriority, TaskSlot, TaskSlotPool, TaskSlotType, CLEANUP_INTERVAL,
    STALE_RELEASE_THRESHOLD, STALE_WARNING_THRESHOLD,
};
pub use transfer::{
    ShareLink, SharePageInfo, SharedFileInfo, TransferError, TransferManager, TransferResult,
    TransferStatus, TransferTask,
};
pub use uploader::{
    PcsServerHealthManager, RapidUploadChecker, RapidUploadHash, UploadEngine, UploadManager,
    UploadTask, UploadTaskStatus,
};
