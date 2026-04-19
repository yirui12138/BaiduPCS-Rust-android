// @vitest-environment happy-dom
// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from './auth'
import {
  generateQRCode,
  getCurrentUser,
  getQRCodeStatus,
} from '@/api/auth'

vi.mock('@/api/auth', () => ({
  cookieLogin: vi.fn(),
  generateQRCode: vi.fn(),
  getCurrentUser: vi.fn(),
  getQRCodeStatus: vi.fn(),
  logout: vi.fn(),
}))

const mockQRCode = {
  sign: 'qr-sign',
  image_base64: 'data:image/png;base64,abc',
  qrcode_url: 'https://example.test/qr',
  created_at: 1,
}

const mockUser = {
  uid: 1,
  username: 'tester',
  bduss: 'bduss',
  login_time: 1,
}

describe('auth store QR login status', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useFakeTimers()
    vi.mocked(generateQRCode).mockResolvedValue(mockQRCode)
    vi.mocked(getCurrentUser).mockResolvedValue(mockUser)
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.clearAllMocks()
  })

  it('resets QR status to waiting after generating a new code', async () => {
    const store = useAuthStore()

    await store.generateQRCode()

    expect(store.qrLoginStatus).toBe('waiting')
    expect(store.isQrScanned).toBe(false)
    expect(store.loginFinalizing).toBe(false)
  })

  it('keeps scanned status visible while waiting for phone confirmation', async () => {
    vi.mocked(getQRCodeStatus).mockResolvedValue({ status: 'scanned' })
    const store = useAuthStore()
    await store.generateQRCode()
    const onScanned = vi.fn()

    store.startPolling(vi.fn(), vi.fn(), onScanned)

    await vi.waitFor(() => {
      expect(store.qrLoginStatus).toBe('scanned')
    })
    expect(store.isQrScanned).toBe(true)
    expect(onScanned).toHaveBeenCalledTimes(1)
  })

  it('moves from scanned to success and syncs the login session', async () => {
    vi.mocked(getQRCodeStatus)
      .mockResolvedValueOnce({ status: 'scanned' })
      .mockResolvedValueOnce({ status: 'success', user: mockUser, token: 'bduss' })
    const store = useAuthStore()
    await store.generateQRCode()
    const onSuccess = vi.fn()

    store.startPolling(onSuccess, vi.fn(), vi.fn(), { pollIntervalMs: 1500 })
    await vi.waitFor(() => {
      expect(store.qrLoginStatus).toBe('scanned')
    })

    await vi.advanceTimersByTimeAsync(1500)

    await vi.waitFor(() => {
      expect(store.qrLoginStatus).toBe('success')
      expect(store.loginFinalizing).toBe(true)
      expect(store.loginFinalizingMessage).toBe('登录完成，正在进入文件页')
      expect(onSuccess).toHaveBeenCalledTimes(1)
    })
  })

  it('can poll QR status immediately when the app returns to foreground', async () => {
    vi.mocked(getQRCodeStatus)
      .mockResolvedValueOnce({ status: 'waiting' })
      .mockResolvedValueOnce({ status: 'scanned' })
    const store = useAuthStore()
    await store.generateQRCode()
    const onScanned = vi.fn()

    store.startPolling(vi.fn(), vi.fn(), onScanned, { pollIntervalMs: 5000 })
    await vi.waitFor(() => {
      expect(getQRCodeStatus).toHaveBeenCalledTimes(1)
    })

    await store.pollQRCodeStatusNow()

    expect(store.qrLoginStatus).toBe('scanned')
    expect(onScanned).toHaveBeenCalledTimes(1)
  })

  it('can clear finalizing state after navigation', async () => {
    const store = useAuthStore()

    store.setLoginFinalizing(true, '授权成功，正在同步登录')
    store.setLoginFinalizing(false)

    expect(store.loginFinalizing).toBe(false)
    expect(store.loginFinalizingMessage).toBe('')
  })

  it('does not leave the scanned overlay visible after expiry', async () => {
    vi.mocked(getQRCodeStatus).mockResolvedValue({ status: 'expired' })
    const store = useAuthStore()
    await store.generateQRCode()
    const onError = vi.fn()

    store.startPolling(vi.fn(), onError, vi.fn())

    await vi.waitFor(() => {
      expect(store.qrLoginStatus).toBe('expired')
    })
    expect(store.isQrScanned).toBe(false)
    expect(onError).toHaveBeenCalledTimes(1)
  })
})
