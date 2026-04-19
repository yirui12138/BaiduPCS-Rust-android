// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

import { onBeforeUnmount, onMounted, ref } from 'vue'
import {
  ANDROID_VPN_STATUS_EVENT,
  hasAndroidBridge,
  isVpnActiveInAndroid,
  type AndroidVpnStatusDetail,
} from '@/utils/androidBridge'

export function useAndroidVpnWarning() {
  const vpnWarningVisible = ref(false)
  let warnedInCurrentVpnSession = false

  function applyVpnState(active: boolean) {
    if (!active) {
      warnedInCurrentVpnSession = false
      vpnWarningVisible.value = false
      return
    }

    if (!warnedInCurrentVpnSession) {
      warnedInCurrentVpnSession = true
      vpnWarningVisible.value = true
    }
  }

  function handleVpnStatus(event: Event) {
    const detail = (event as CustomEvent<AndroidVpnStatusDetail>).detail
    if (!detail || typeof detail.active !== 'boolean') return

    applyVpnState(detail.active)
  }

  function dismissVpnWarning() {
    vpnWarningVisible.value = false
  }

  onMounted(() => {
    if (!hasAndroidBridge()) return

    window.addEventListener(ANDROID_VPN_STATUS_EVENT, handleVpnStatus as EventListener)

    const initialState = isVpnActiveInAndroid()
    if (initialState !== null) {
      applyVpnState(initialState)
    }
  })

  onBeforeUnmount(() => {
    window.removeEventListener(ANDROID_VPN_STATUS_EVENT, handleVpnStatus as EventListener)
  })

  return {
    vpnWarningVisible,
    dismissVpnWarning,
  }
}
