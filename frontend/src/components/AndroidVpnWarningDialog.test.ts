// @vitest-environment happy-dom
// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { afterEach, describe, expect, it } from 'vitest'
import AndroidVpnWarningDialog from './AndroidVpnWarningDialog.vue'

afterEach(() => {
  document.body.innerHTML = ''
})

describe('AndroidVpnWarningDialog', () => {
  it('renders a compact VPN warning card with the approved copy', () => {
    mount(AndroidVpnWarningDialog, {
      props: { modelValue: true },
      attachTo: document.body,
      global: {
        stubs: {
          'el-button': { template: '<button><slot /></button>' },
          'el-icon': { template: '<span><slot /></span>' },
        },
      },
    })

    const card = document.body.querySelector('.vpn-warning-card')

    expect(card).not.toBeNull()
    expect(document.body.textContent).toContain('VPN 环境提示')
    expect(document.body.textContent).toContain(
      '我们无意冒犯您的互联网自由，但本软件在vpn环境下尚不稳定，您依然可以使用本软件，但关闭vpn可以提升稳定性',
    )
    expect(document.body.textContent).toContain('我知道了')
  })

  it('emits a model update when the user acknowledges the warning', async () => {
    const wrapper = mount(AndroidVpnWarningDialog, {
      props: { modelValue: true },
      attachTo: document.body,
      global: {
        stubs: {
          'el-button': { template: '<button><slot /></button>' },
          'el-icon': { template: '<span><slot /></span>' },
        },
      },
    })

    const action = document.body.querySelector('.vpn-warning-action') as HTMLElement
    action.click()
    await nextTick()

    expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
  })
})
