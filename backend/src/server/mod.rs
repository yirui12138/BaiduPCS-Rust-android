// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

// Web服务器模块

pub mod error;
pub mod events;
pub mod handlers;
pub mod state;
pub mod websocket;

pub use error::{ApiError, ApiResult};
pub use state::AppState;
pub use websocket::WebSocketManager;
