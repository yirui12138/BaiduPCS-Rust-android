// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

pub mod chunk;
pub mod engine;
pub mod folder;
pub mod folder_manager;
pub mod manager;
pub mod progress;
pub mod scheduler;
pub mod task;

pub use chunk::{Chunk, ChunkManager};
pub use engine::{DownloadEngine, UrlHealthManager};
pub use folder::{FolderDownload, FolderStatus, PendingFile};
pub use folder_manager::FolderDownloadManager;
pub use manager::DownloadManager;
pub use progress::SpeedCalculator;
pub use scheduler::{calculate_task_max_chunks, ChunkScheduler, TaskScheduleInfo};
pub use task::{DownloadTask, TaskStatus};

// Re-export conflict strategy from uploader module for convenience
pub use crate::uploader::conflict::DownloadConflictStrategy;
