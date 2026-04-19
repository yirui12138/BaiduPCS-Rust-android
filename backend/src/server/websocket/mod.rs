// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

//! WebSocket 模块
//!
//! 提供 WebSocket 实时推送功能

mod handler;
pub mod manager;
mod message;

pub use handler::handle_websocket;
pub use manager::{WebSocketManager, PendingEvent, MAX_PENDING_EVENTS_PER_CONNECTION};
pub use message::{WsClientMessage, WsServerMessage};
