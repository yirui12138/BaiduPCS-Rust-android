// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

//! 加密模块

pub mod buffer_pool;
pub mod config_store;
pub mod export;
pub mod service;
pub mod snapshot;

pub use buffer_pool::{BufferPool, PooledBuffer, BufferPoolStats};
pub use config_store::{EncryptionConfigStore, EncryptionKeyConfig, EncryptionKeyInfo};
pub use export::{DecryptBundleExporter, MappingExport, MappingGenerator, MappingRecord};
pub use service::{EncryptionService, StreamingEncryptionService, ENCRYPTED_FILE_EXTENSION};
pub use snapshot::SnapshotManager;
