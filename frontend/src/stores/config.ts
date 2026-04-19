// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getConfig, updateConfig, type AppConfig } from '@/api/config'

export const useConfigStore = defineStore('config', () => {
  // 状态
  const config = ref<AppConfig | null>(null)
  const loading = ref(false)

  // 获取配置
  async function fetchConfig() {
    loading.value = true
    try {
      config.value = await getConfig()
      return config.value
    } catch (error) {
      console.error('获取配置失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // 更新配置
  async function saveConfig(newConfig: AppConfig) {
    loading.value = true
    try {
      await updateConfig(newConfig)
      config.value = newConfig
    } catch (error) {
      console.error('更新配置失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  return {
    // 状态
    config,
    loading,

    // 方法
    fetchConfig,
    saveConfig,
  }
})

