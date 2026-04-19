// @vitest-environment happy-dom
// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

import { mount } from '@vue/test-utils'
import { defineComponent, nextTick } from 'vue'
import { afterEach, describe, expect, it } from 'vitest'
import { ANDROID_VPN_STATUS_EVENT, type AndroidWebBridge } from '@/utils/androidBridge'
import { useAndroidVpnWarning } from './useAndroidVpnWarning'

type HarnessVm = {
  vpnWarningVisible: boolean
  dismissVpnWarning: () => void
}

const Harness = defineComponent({
  setup() {
    return useAndroidVpnWarning()
  },
  template: '<div>{{ vpnWarningVisible }}</div>',
})

function installBridge(isVpnActive: AndroidWebBridge['isVpnActive']) {
  window.BaiduPCSAndroid = {
    openFolder: () => false,
    importFiles: () => false,
    importFolder: () => false,
    isVpnActive,
  }
}

function dispatchVpnStatus(active: boolean) {
  window.dispatchEvent(new CustomEvent(ANDROID_VPN_STATUS_EVENT, {
    detail: { active },
  }))
}

async function mountHarness() {
  const wrapper = mount(Harness)
  await nextTick()
  return wrapper
}

afterEach(() => {
  delete window.BaiduPCSAndroid
  document.body.innerHTML = ''
})

describe('useAndroidVpnWarning', () => {
  it('shows the warning when Android reports an active VPN at startup', async () => {
    installBridge(() => true)

    const wrapper = await mountHarness()

    expect((wrapper.vm as unknown as HarnessVm).vpnWarningVisible).toBe(true)
  })

  it('does not show the warning when Android reports no active VPN', async () => {
    installBridge(() => false)

    const wrapper = await mountHarness()

    expect((wrapper.vm as unknown as HarnessVm).vpnWarningVisible).toBe(false)
  })

  it('only warns once during the same active VPN session', async () => {
    installBridge(() => false)
    const wrapper = await mountHarness()
    const vm = wrapper.vm as unknown as HarnessVm

    dispatchVpnStatus(true)
    await nextTick()
    expect(vm.vpnWarningVisible).toBe(true)

    vm.dismissVpnWarning()
    await nextTick()
    dispatchVpnStatus(true)
    await nextTick()

    expect(vm.vpnWarningVisible).toBe(false)
  })

  it('allows a new warning after VPN is disabled and enabled again', async () => {
    installBridge(() => false)
    const wrapper = await mountHarness()
    const vm = wrapper.vm as unknown as HarnessVm

    dispatchVpnStatus(true)
    await nextTick()
    vm.dismissVpnWarning()
    dispatchVpnStatus(false)
    await nextTick()
    dispatchVpnStatus(true)
    await nextTick()

    expect(vm.vpnWarningVisible).toBe(true)
  })
})
