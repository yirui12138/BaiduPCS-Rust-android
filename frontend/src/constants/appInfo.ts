// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

export const APP_DISPLAY_NAME = '柏渡云盘'
export const APP_LEGAL_PAGE_NAME = '开源许可与鸣谢'
export const APP_FALLBACK_VERSION = '1.0.0'
export const UPSTREAM_PROJECT_NAME = 'BaiduPCS-Rust'
export const UPSTREAM_AUTHOR = 'komorebiCarry'
export const UPSTREAM_VERSION = 'v1.12.1'
export const UPSTREAM_RELEASE_URL =
  'https://github.com/komorebiCarry/BaiduPCS-Rust/releases/tag/v1.12.1'
export const APP_NON_OFFICIAL_NOTICE =
  '本应用为独立 Android 移植版，非上游官方发布，也非相关品牌官方客户端。'

const SHELL_BUILD_CACHE_KEY = 'baidupcs-android-shell-build'

export function getAppVersion(): string {
  if (typeof window === 'undefined') {
    return APP_FALLBACK_VERSION
  }

  const params = new URLSearchParams(window.location.search)
  const build = params.get('shellBuild')
  if (build) {
    window.sessionStorage.setItem(SHELL_BUILD_CACHE_KEY, build)
    return build
  }

  return window.sessionStorage.getItem(SHELL_BUILD_CACHE_KEY) || APP_FALLBACK_VERSION
}
