// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  cookieLogin as apiCookieLogin,
  generateQRCode as apiGenerateQRCode,
  getCurrentUser,
  getQRCodeStatus,
  logout as apiLogout,
} from '@/api/auth'
import type { CookieLoginResult, QRCode, UserAuth } from '@/api/auth'

export type QRLoginStatus = 'idle' | 'waiting' | 'scanned' | 'success' | 'expired' | 'failed'

export interface StartQrPollingOptions {
  pollIntervalMs?: number
}

interface QrPollingHandlers {
  onSuccess: () => void
  onError: (error: any) => void
  onScanned?: () => void
}

function delay(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserAuth | null>(null)
  const qrcode = ref<QRCode | null>(null)
  const qrLoginStatus = ref<QRLoginStatus>('idle')
  const isPolling = ref(false)
  const pollingTimer = ref<number | null>(null)
  const loginFinalizing = ref(false)
  const loginFinalizingMessage = ref('')
  let qrPollingHandlers: QrPollingHandlers | null = null
  let qrPollInFlight = false
  let qrPollPending = false
  let qrTerminalHandled = false
  let qrScannedNotified = false

  const isLoggedIn = computed(() => user.value !== null)
  const username = computed(() => user.value?.nickname || user.value?.username || '')
  const avatar = computed(() => user.value?.avatar_url || '')
  const isQrScanned = computed(() => qrLoginStatus.value === 'scanned')

  function setLoginFinalizing(active: boolean, message = '') {
    loginFinalizing.value = active
    loginFinalizingMessage.value = active ? message : ''
  }

  async function refreshUserInfo(options?: {
    retryCount?: number
    retryDelayMs?: number
    preserveUserOnFailure?: boolean
  }): Promise<UserAuth> {
    const retryCount = options?.retryCount ?? 0
    const retryDelayMs = options?.retryDelayMs ?? 0
    const preserveUserOnFailure = options?.preserveUserOnFailure ?? false
    let lastError: unknown = null

    for (let attempt = 0; attempt <= retryCount; attempt++) {
      try {
        user.value = await getCurrentUser()
        return user.value
      } catch (error) {
        lastError = error

        if (attempt < retryCount) {
          await delay(retryDelayMs)
        }
      }
    }

    if (!preserveUserOnFailure) {
      user.value = null
    }

    throw lastError
  }

  async function syncLoginSession(fallbackUser?: UserAuth | null): Promise<UserAuth | null> {
    try {
      return await refreshUserInfo({
        retryCount: 6,
        retryDelayMs: 1000,
        preserveUserOnFailure: true,
      })
    } catch (error) {
      console.error('同步登录会话失败:', error)
      if (fallbackUser) {
        user.value = fallbackUser
        return fallbackUser
      }
      return user.value
    }
  }

  async function ensureSession(): Promise<boolean> {
    try {
      await refreshUserInfo({
        retryCount: 1,
        retryDelayMs: 400,
        preserveUserOnFailure: false,
      })
      return true
    } catch {
      return false
    }
  }

  async function generateQRCode(): Promise<QRCode> {
    try {
      setLoginFinalizing(false)
      qrcode.value = await apiGenerateQRCode()
      qrLoginStatus.value = 'waiting'
      qrTerminalHandled = false
      qrScannedNotified = false
      return qrcode.value
    } catch (error) {
      qrLoginStatus.value = 'failed'
      console.error('生成二维码失败:', error)
      throw error
    }
  }

  async function pollQRCodeStatusNow() {
    if (!qrcode.value || !qrPollingHandlers || qrTerminalHandled) return

    if (qrPollInFlight) {
      qrPollPending = true
      return
    }

    qrPollInFlight = true

    do {
      qrPollPending = false
      const handlers: QrPollingHandlers | null = qrPollingHandlers
      const currentQr: QRCode | null = qrcode.value
      if (!currentQr || !handlers || qrTerminalHandled) break

      try {
        const polledSign: string = currentQr.sign
        const status = await getQRCodeStatus(polledSign)
        if (
          qrTerminalHandled ||
          qrPollingHandlers !== handlers ||
          !isPolling.value ||
          qrcode.value?.sign !== polledSign
        ) {
          break
        }

        switch (status.status) {
          case 'success': {
            qrTerminalHandled = true
            qrLoginStatus.value = 'success'
            stopPolling()
            setLoginFinalizing(true, '授权成功，正在同步登录')
            await syncLoginSession(status.user)
            loginFinalizingMessage.value = '登录完成，正在进入文件页'
            handlers.onSuccess()
            break
          }
          case 'expired':
            qrTerminalHandled = true
            qrLoginStatus.value = 'expired'
            stopPolling()
            setLoginFinalizing(false)
            handlers.onError(new Error('二维码已过期'))
            break
          case 'failed':
            qrTerminalHandled = true
            qrLoginStatus.value = 'failed'
            stopPolling()
            setLoginFinalizing(false)
            handlers.onError(new Error(status.reason || '登录失败'))
            break
          case 'scanned':
            qrLoginStatus.value = 'scanned'
            if (!qrScannedNotified) {
              qrScannedNotified = true
              handlers.onScanned?.()
            }
            break
          case 'waiting':
            if (qrLoginStatus.value !== 'scanned') qrLoginStatus.value = 'waiting'
            break
        }
      } catch (error) {
        console.error('轮询失败:', error)
      }
    } while (qrPollPending)

    qrPollInFlight = false
  }

  function startPolling(
    onSuccess: () => void,
    onError: (error: any) => void,
    onScanned?: () => void,
    options?: StartQrPollingOptions,
  ) {
    if (!qrcode.value) return

    stopPolling()
    qrPollingHandlers = { onSuccess, onError, onScanned }
    isPolling.value = true
    qrTerminalHandled = false
    qrScannedNotified = qrLoginStatus.value === 'scanned'
    const pollIntervalMs = options?.pollIntervalMs ?? 3000

    void pollQRCodeStatusNow()
    pollingTimer.value = window.setInterval(() => {
      void pollQRCodeStatusNow()
    }, pollIntervalMs)
  }

  function stopPolling() {
    if (pollingTimer.value) {
      clearInterval(pollingTimer.value)
      pollingTimer.value = null
    }
    isPolling.value = false
    qrPollingHandlers = null
  }

  async function fetchUserInfo() {
    try {
      await refreshUserInfo({
        retryCount: 0,
        retryDelayMs: 0,
        preserveUserOnFailure: false,
      })
    } catch (error) {
      console.error('获取用户信息失败:', error)
      throw error
    }
  }

  async function loginWithCookies(cookies: string): Promise<CookieLoginResult> {
    try {
      setLoginFinalizing(true, 'Cookie 已导入，正在同步登录')
      const result = await apiCookieLogin(cookies)
      user.value = result.user
      loginFinalizingMessage.value = '登录完成，正在进入文件页'
      return result
    } catch (error) {
      setLoginFinalizing(false)
      console.error('Cookie 登录失败:', error)
      throw error
    }
  }

  async function logout() {
    try {
      await apiLogout()
      user.value = null
      qrcode.value = null
      qrLoginStatus.value = 'idle'
      setLoginFinalizing(false)
      stopPolling()
    } catch (error) {
      console.error('登出失败:', error)
      throw error
    }
  }

  return {
    user,
    qrcode,
    qrLoginStatus,
    isPolling,
    loginFinalizing,
    loginFinalizingMessage,
    isLoggedIn,
    username,
    avatar,
    isQrScanned,
    setLoginFinalizing,
    generateQRCode,
    startPolling,
    pollQRCodeStatusNow,
    stopPolling,
    fetchUserInfo,
    refreshUserInfo,
    syncLoginSession,
    ensureSession,
    loginWithCookies,
    logout,
  }
})
