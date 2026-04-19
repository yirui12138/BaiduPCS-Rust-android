<!--
SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
SPDX-License-Identifier: Apache-2.0

This file is part of the Android port in this repository.
Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
for Android integration, mobile UX, and distribution compliance.
See the repository LICENSE and NOTICE files for details.
-->

<template>
  <teleport to="body">
    <transition name="vpn-warning-fade">
      <div v-if="modelValue" class="vpn-warning-layer">
        <section
          class="vpn-warning-card"
          role="dialog"
          aria-modal="true"
          aria-labelledby="vpn-warning-title"
        >
          <div class="vpn-warning-icon" aria-hidden="true">
            <el-icon>
              <WarningFilled />
            </el-icon>
          </div>
          <div class="vpn-warning-copy">
            <h2 id="vpn-warning-title">VPN 环境提示</h2>
            <p>我们无意冒犯您的互联网自由，但本软件在vpn环境下尚不稳定，您依然可以使用本软件，但关闭vpn可以提升稳定性</p>
          </div>
          <el-button class="vpn-warning-action" type="primary" round @click="close">
            我知道了
          </el-button>
        </section>
      </div>
    </transition>
  </teleport>
</template>

<script setup lang="ts">
import { WarningFilled } from '@element-plus/icons-vue'

defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

function close() {
  emit('update:modelValue', false)
}
</script>

<style scoped lang="scss">
.vpn-warning-layer {
  position: fixed;
  inset: 0;
  z-index: 3200;
  display: grid;
  place-items: center;
  padding: calc(env(safe-area-inset-top, 0px) + 16px) 20px calc(env(safe-area-inset-bottom, 0px) + 20px);
  background: rgba(15, 23, 42, 0.16);
  backdrop-filter: blur(2px);
}

.vpn-warning-card {
  width: min(320px, calc(100vw - 40px));
  max-width: 360px;
  box-sizing: border-box;
  display: grid;
  gap: 12px;
  justify-items: center;
  padding: 18px 18px 16px;
  border: 1px solid var(--el-border-color-light);
  border-color: color-mix(in srgb, var(--el-border-color-light) 70%, transparent);
  border-radius: 22px;
  background:
    radial-gradient(circle at top left, rgba(245, 158, 11, 0.16), transparent 38%),
    var(--el-bg-color);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.22);
  color: var(--el-text-color-primary);
}

.vpn-warning-icon {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: rgba(245, 158, 11, 0.14);
  background: color-mix(in srgb, var(--el-color-warning) 18%, transparent);
  color: var(--el-color-warning);
  font-size: 22px;
}

.vpn-warning-copy {
  display: grid;
  gap: 8px;
  text-align: center;

  h2 {
    margin: 0;
    font-size: 17px;
    line-height: 1.25;
    font-weight: 700;
  }

  p {
    margin: 0;
    font-size: 13px;
    line-height: 1.65;
    color: var(--el-text-color-regular);
  }
}

.vpn-warning-action {
  width: 132px;
  min-height: 36px;
  margin-top: 2px;
  font-weight: 700;
}

.vpn-warning-fade-enter-active,
.vpn-warning-fade-leave-active {
  transition: opacity 0.18s ease;

  .vpn-warning-card {
    transition: transform 0.18s ease, opacity 0.18s ease;
  }
}

.vpn-warning-fade-enter-from,
.vpn-warning-fade-leave-to {
  opacity: 0;

  .vpn-warning-card {
    opacity: 0;
    transform: translateY(8px) scale(0.98);
  }
}

@media (min-width: 640px) {
  .vpn-warning-card {
    width: 360px;
  }
}
</style>
