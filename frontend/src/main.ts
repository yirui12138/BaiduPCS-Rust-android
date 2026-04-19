// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import './styles/main.scss'
import './styles/mobile.scss'

function syncSystemTheme() {
  if (typeof window === 'undefined') return

  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  const applyTheme = () => {
    document.documentElement.classList.toggle('dark', mediaQuery.matches)
    document.documentElement.dataset.theme = mediaQuery.matches ? 'dark' : 'light'
  }

  applyTheme()

  if (mediaQuery.addEventListener) {
    mediaQuery.addEventListener('change', applyTheme)
  } else {
    mediaQuery.addListener(applyTheme)
  }
}

const app = createApp(App)
const pinia = createPinia()

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus)

// 初始化 Web 认证 Store（在 pinia 安装后）
import { useWebAuthStore } from './stores/webAuth'
const webAuthStore = useWebAuthStore()
webAuthStore.initialize()

syncSystemTheme()

app.mount('#app')
