// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

export interface AndroidWebBridge {
  openFolder(path: string, requestId: string): boolean
  importFiles(): boolean
  importFolder(): boolean
  cleanupImportedPaths?(pathsJson: string): boolean
  cleanupStaleImports?(): boolean
  readClipboardText?(): string
  isVpnActive?(): boolean
  startBaiduCookieLogin?(): boolean
}

export interface AndroidImportedEntry {
  name: string
  path: string
  entryType: 'file' | 'directory'
}

export interface AndroidImportCompleteDetail {
  sourceType: 'file' | 'directory'
  count: number
  entries: AndroidImportedEntry[]
}

export interface AndroidOpenFolderResultDetail {
  requestId: string
  status: 'opened' | 'failed'
  path: string
  reason?: string
}

export interface AndroidVpnStatusDetail {
  active: boolean
}

export interface AndroidCookieLoginResultDetail {
  status: 'success' | 'cancelled' | 'failed'
  cookies?: string
  reason?: string
}

export const ANDROID_IMPORT_COMPLETE_EVENT = 'android-import-complete'
export const ANDROID_OPEN_FOLDER_RESULT_EVENT = 'android-open-folder-result'
export const ANDROID_VPN_STATUS_EVENT = 'android-vpn-status'
export const ANDROID_COOKIE_LOGIN_RESULT_EVENT = 'android-cookie-login-result'

const OPEN_FOLDER_TIMEOUT_MS = 1200

let openFolderRequestSequence = 0

declare global {
  interface Window {
    BaiduPCSAndroid?: AndroidWebBridge
  }
}

export function hasAndroidBridge(): boolean {
  return typeof window !== 'undefined' && typeof window.BaiduPCSAndroid !== 'undefined'
}

export function canOpenFolderInAndroid(): boolean {
  return hasAndroidBridge() && typeof window.BaiduPCSAndroid?.openFolder === 'function'
}

export function canImportFromAndroid(): boolean {
  return hasAndroidBridge() &&
    typeof window.BaiduPCSAndroid?.importFiles === 'function' &&
    typeof window.BaiduPCSAndroid?.importFolder === 'function'
}

export function canCleanupImportedPathsInAndroid(): boolean {
  return hasAndroidBridge() && typeof window.BaiduPCSAndroid?.cleanupImportedPaths === 'function'
}

export function cleanupImportedPathsInAndroid(paths: string[]): boolean {
  if (!canCleanupImportedPathsInAndroid() || paths.length === 0) return false

  try {
    return Boolean(window.BaiduPCSAndroid?.cleanupImportedPaths?.(JSON.stringify(paths)))
  } catch (error) {
    console.error('Failed to request Android imported file cleanup', error)
    return false
  }
}

export function cleanupStaleImportsInAndroid(): boolean {
  if (!hasAndroidBridge() || typeof window.BaiduPCSAndroid?.cleanupStaleImports !== 'function') return false

  try {
    return Boolean(window.BaiduPCSAndroid.cleanupStaleImports())
  } catch (error) {
    console.error('Failed to request Android stale import cleanup', error)
    return false
  }
}

export function canCheckVpnInAndroid(): boolean {
  return hasAndroidBridge() && typeof window.BaiduPCSAndroid?.isVpnActive === 'function'
}

export function canReadClipboardInAndroid(): boolean {
  return hasAndroidBridge() && typeof window.BaiduPCSAndroid?.readClipboardText === 'function'
}

export function canStartBaiduCookieLoginInAndroid(): boolean {
  return hasAndroidBridge() && typeof window.BaiduPCSAndroid?.startBaiduCookieLogin === 'function'
}

export function readClipboardTextInAndroid(): string | null {
  if (!canReadClipboardInAndroid()) return null

  try {
    return window.BaiduPCSAndroid?.readClipboardText?.() || ''
  } catch (error) {
    console.error('Failed to read Android clipboard text', error)
    return null
  }
}

export function isVpnActiveInAndroid(): boolean | null {
  if (!canCheckVpnInAndroid()) return null

  try {
    return Boolean(window.BaiduPCSAndroid?.isVpnActive?.())
  } catch (error) {
    console.error('Failed to query Android VPN state', error)
    return null
  }
}

export function startBaiduCookieLoginInAndroid(): boolean {
  if (!canStartBaiduCookieLoginInAndroid()) return false

  try {
    return Boolean(window.BaiduPCSAndroid?.startBaiduCookieLogin?.())
  } catch (error) {
    console.error('Failed to start Android Baidu cookie login', error)
    return false
  }
}

export async function requestOpenFolderInAndroid(path: string): Promise<boolean> {
  if (!canOpenFolderInAndroid() || !path) return false

  const requestId = `open-folder-${Date.now()}-${++openFolderRequestSequence}`

  return new Promise((resolve) => {
    let settled = false
    let timeoutId: number | null = null

    const cleanup = () => {
      window.removeEventListener(ANDROID_OPEN_FOLDER_RESULT_EVENT, handleResult as EventListener)
      document.removeEventListener('visibilitychange', handleVisibilityChange)
      if (timeoutId !== null) {
        window.clearTimeout(timeoutId)
      }
    }

    const finish = (opened: boolean) => {
      if (settled) return
      settled = true
      cleanup()
      resolve(opened)
    }

    const handleResult = (event: Event) => {
      const detail = (event as CustomEvent<AndroidOpenFolderResultDetail>).detail
      if (!detail || detail.requestId !== requestId) return

      console.info('[androidBridge] folder open result', detail)
      finish(detail.status === 'opened')
    }

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'hidden') {
        finish(true)
      }
    }

    window.addEventListener(ANDROID_OPEN_FOLDER_RESULT_EVENT, handleResult as EventListener)
    document.addEventListener('visibilitychange', handleVisibilityChange)

    timeoutId = window.setTimeout(() => {
      console.warn('[androidBridge] folder open request timed out', { requestId, path })
      finish(false)
    }, OPEN_FOLDER_TIMEOUT_MS)

    try {
      const accepted = Boolean(window.BaiduPCSAndroid?.openFolder(path, requestId))
      if (!accepted) {
        console.warn('[androidBridge] folder open request was rejected', { requestId, path })
        finish(false)
      }
    } catch (error) {
      console.error('[androidBridge] failed to request Android folder open', error)
      finish(false)
    }
  })
}

export function importFilesFromAndroid(): boolean {
  if (!canImportFromAndroid()) return false

  try {
    return Boolean(window.BaiduPCSAndroid?.importFiles())
  } catch (error) {
    console.error('Failed to invoke Android file import', error)
    return false
  }
}

export function importFolderFromAndroid(): boolean {
  if (!canImportFromAndroid()) return false

  try {
    return Boolean(window.BaiduPCSAndroid?.importFolder())
  } catch (error) {
    console.error('Failed to invoke Android directory import', error)
    return false
  }
}
