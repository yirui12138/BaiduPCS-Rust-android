// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

import { onMounted, onUnmounted, ref } from 'vue'

export function isDocumentVisible(): boolean {
  if (typeof document === 'undefined') {
    return true
  }
  return document.visibilityState !== 'hidden'
}

export function usePageVisibility() {
  const visible = ref(isDocumentVisible())

  const sync = () => {
    visible.value = isDocumentVisible()
  }

  onMounted(() => {
    document.addEventListener('visibilitychange', sync)
    window.addEventListener('focus', sync)
    window.addEventListener('blur', sync)
  })

  onUnmounted(() => {
    document.removeEventListener('visibilitychange', sync)
    window.removeEventListener('focus', sync)
    window.removeEventListener('blur', sync)
  })

  return visible
}
