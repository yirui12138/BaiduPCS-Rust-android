// @vitest-environment happy-dom
// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  ANDROID_COOKIE_LOGIN_RESULT_EVENT,
  ANDROID_OPEN_FOLDER_RESULT_EVENT,
  canCheckVpnInAndroid,
  canReadClipboardInAndroid,
  canStartBaiduCookieLoginInAndroid,
  cleanupImportedPathsInAndroid,
  cleanupStaleImportsInAndroid,
  isVpnActiveInAndroid,
  readClipboardTextInAndroid,
  requestOpenFolderInAndroid,
  startBaiduCookieLoginInAndroid,
  type AndroidWebBridge,
} from './androidBridge'

function installBridge(
  openFolder: AndroidWebBridge['openFolder'],
  isVpnActive?: AndroidWebBridge['isVpnActive'],
  cleanupImportedPaths?: AndroidWebBridge['cleanupImportedPaths'],
  cleanupStaleImports?: AndroidWebBridge['cleanupStaleImports'],
  readClipboardText?: AndroidWebBridge['readClipboardText'],
  startBaiduCookieLogin?: AndroidWebBridge['startBaiduCookieLogin'],
) {
  window.BaiduPCSAndroid = {
    openFolder,
    importFiles: () => false,
    importFolder: () => false,
    isVpnActive,
    cleanupImportedPaths,
    cleanupStaleImports,
    readClipboardText,
    startBaiduCookieLogin,
  }
}

describe('androidBridge requestOpenFolderInAndroid', () => {
  afterEach(() => {
    delete window.BaiduPCSAndroid
    vi.useRealTimers()
  })

  it('resolves true when Android reports the folder was opened', async () => {
    installBridge((path, requestId) => {
      window.dispatchEvent(new CustomEvent(ANDROID_OPEN_FOLDER_RESULT_EVENT, {
        detail: { requestId, status: 'opened', path },
      }))
      return true
    })

    await expect(requestOpenFolderInAndroid('/storage/emulated/0/Download/BaiduPCS'))
      .resolves.toBe(true)
  })

  it('resolves false when Android reports opening failed', async () => {
    installBridge((path, requestId) => {
      window.dispatchEvent(new CustomEvent(ANDROID_OPEN_FOLDER_RESULT_EVENT, {
        detail: { requestId, status: 'failed', path, reason: 'launch_failed' },
      }))
      return true
    })

    await expect(requestOpenFolderInAndroid('/storage/emulated/0/Download/BaiduPCS'))
      .resolves.toBe(false)
  })

  it('resolves false when Android never sends a result', async () => {
    vi.useFakeTimers()
    installBridge(() => true)

    const result = requestOpenFolderInAndroid('/storage/emulated/0/Download/BaiduPCS')
    await vi.advanceTimersByTimeAsync(1300)

    await expect(result).resolves.toBe(false)
  })
})

describe('androidBridge imported cleanup helpers', () => {
  afterEach(() => {
    delete window.BaiduPCSAndroid
  })

  it('passes imported cleanup paths to Android as JSON', () => {
    const cleanupImportedPaths = vi.fn(() => true)
    installBridge(() => false, undefined, cleanupImportedPaths)

    expect(cleanupImportedPathsInAndroid(['/storage/emulated/0/Android/data/app/files/Documents/UploadSpace/a.txt']))
      .toBe(true)
    expect(cleanupImportedPaths).toHaveBeenCalledWith(
      JSON.stringify(['/storage/emulated/0/Android/data/app/files/Documents/UploadSpace/a.txt']),
    )
  })

  it('can request stale import cleanup when the bridge supports it', () => {
    const cleanupStaleImports = vi.fn(() => true)
    installBridge(() => false, undefined, undefined, cleanupStaleImports)

    expect(cleanupStaleImportsInAndroid()).toBe(true)
    expect(cleanupStaleImports).toHaveBeenCalledTimes(1)
  })
})

describe('androidBridge VPN helpers', () => {
  afterEach(() => {
    delete window.BaiduPCSAndroid
  })

  it('reports whether the Android VPN bridge is available', () => {
    expect(canCheckVpnInAndroid()).toBe(false)

    installBridge(() => false, () => true)

    expect(canCheckVpnInAndroid()).toBe(true)
  })

  it('returns the Android VPN state when available', () => {
    installBridge(() => false, () => true)

    expect(isVpnActiveInAndroid()).toBe(true)
  })

  it('returns null when the Android VPN bridge is unavailable', () => {
    installBridge(() => false)

    expect(isVpnActiveInAndroid()).toBeNull()
  })
})

describe('androidBridge clipboard helpers', () => {
  afterEach(() => {
    delete window.BaiduPCSAndroid
  })

  it('reports whether the Android clipboard bridge is available', () => {
    expect(canReadClipboardInAndroid()).toBe(false)

    installBridge(() => false, undefined, undefined, undefined, () => 'hello')

    expect(canReadClipboardInAndroid()).toBe(true)
  })

  it('returns clipboard text when available', () => {
    installBridge(() => false, undefined, undefined, undefined, () => 'https://pan.baidu.com/s/1abc')

    expect(readClipboardTextInAndroid()).toBe('https://pan.baidu.com/s/1abc')
  })
})

describe('androidBridge Baidu cookie login helpers', () => {
  afterEach(() => {
    delete window.BaiduPCSAndroid
  })

  it('reports whether the Android cookie login bridge is available', () => {
    expect(canStartBaiduCookieLoginInAndroid()).toBe(false)

    installBridge(() => false, undefined, undefined, undefined, undefined, () => true)

    expect(canStartBaiduCookieLoginInAndroid()).toBe(true)
  })

  it('starts Android cookie login when available', () => {
    const startBaiduCookieLogin = vi.fn(() => true)
    installBridge(() => false, undefined, undefined, undefined, undefined, startBaiduCookieLogin)

    expect(startBaiduCookieLoginInAndroid()).toBe(true)
    expect(startBaiduCookieLogin).toHaveBeenCalledTimes(1)
  })

  it('can receive Android cookie login result events', () => {
    const listener = vi.fn()
    window.addEventListener(ANDROID_COOKIE_LOGIN_RESULT_EVENT, listener as EventListener)

    window.dispatchEvent(new CustomEvent(ANDROID_COOKIE_LOGIN_RESULT_EVENT, {
      detail: { status: 'success', cookies: 'BDUSS=fake-for-test' },
    }))

    expect(listener).toHaveBeenCalledTimes(1)
    window.removeEventListener(ANDROID_COOKIE_LOGIN_RESULT_EVENT, listener as EventListener)
  })
})
