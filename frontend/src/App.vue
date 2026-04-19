<!--
SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
SPDX-License-Identifier: Apache-2.0

This file is part of the Android port in this repository.
Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
for Android integration, mobile UX, and distribution compliance.
See the repository LICENSE and NOTICE files for details.
-->

<template>
  <div id="app">
    <router-view />
    <AndroidVpnWarningDialog
      v-model="vpnWarningVisible"
      @update:model-value="dismissVpnWarning"
    />
  </div>
</template>

<script setup lang="ts">
import AndroidVpnWarningDialog from '@/components/AndroidVpnWarningDialog.vue'
import { useAndroidVpnWarning } from '@/composables/useAndroidVpnWarning'

const { vpnWarningVisible, dismissVpnWarning } = useAndroidVpnWarning()
</script>

<style scoped>
#app {
  width: 100%;
  min-height: 100vh;
  min-height: 100dvh;
  overflow-x: hidden;
}
</style>
