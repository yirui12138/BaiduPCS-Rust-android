// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

// 认证模块

pub mod constants;
pub mod cookie_login;
pub mod qrcode;
pub mod session;
pub mod types;

pub use cookie_login::CookieLoginAuth;
pub use qrcode::QRCodeAuth;
pub use session::SessionManager;
pub use types::{CookieLoginApiRequest, LoginRequest, LoginResponse, QRCode, QRCodeStatus, UserAuth};
