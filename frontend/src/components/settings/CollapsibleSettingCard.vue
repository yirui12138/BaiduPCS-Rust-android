<!--
SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
SPDX-License-Identifier: Apache-2.0

This file is part of the Android port in this repository.
Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
for Android integration, mobile UX, and distribution compliance.
See the repository LICENSE and NOTICE files for details.
-->

<template>
  <el-card v-if="!isMobile" :id="id" class="setting-card" shadow="hover">
    <template #header>
      <slot name="header">
        <div class="card-header">
          <span>{{ title }}</span>
        </div>
      </slot>
    </template>
    <slot />
  </el-card>

  <section
      v-else
      :id="id"
      class="mobile-setting-section"
      :class="{ 'is-expanded': expanded }"
      :style="{ '--section-color': color }"
  >
    <button class="mobile-setting-trigger" type="button" @click="toggle">
      <span class="mobile-setting-icon">
        <slot name="icon" />
      </span>
      <span class="mobile-setting-title">
        <strong>{{ title }}</strong>
        <small v-if="description">{{ description }}</small>
      </span>
      <el-icon class="mobile-setting-arrow"><ArrowDown /></el-icon>
    </button>

    <div
        class="mobile-setting-body-shell"
        :class="{ 'is-expanded': expanded }"
        :aria-hidden="!expanded"
        :inert="!expanded ? true : undefined"
    >
      <div class="mobile-setting-body">
        <slot />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ArrowDown } from '@element-plus/icons-vue'
import { useIsMobile } from '@/utils/responsive'

const props = withDefaults(defineProps<{
  id: string
  title: string
  description?: string
  color?: string
  expanded?: boolean
}>(), {
  description: '',
  color: '#409eff',
  expanded: false,
})

const emit = defineEmits<{
  'update:expanded': [value: boolean]
}>()

const isMobile = useIsMobile()

function toggle() {
  emit('update:expanded', !props.expanded)
}
</script>

<style scoped lang="scss">
.mobile-setting-section {
  --section-color: #409eff;
  margin-bottom: 10px;
}

.mobile-setting-trigger {
  width: 100%;
  min-height: 56px;
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) 24px;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border: 1px solid var(--app-border);
  border-radius: 18px;
  background: var(--app-surface);
  color: var(--app-text);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.05);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.18s ease;

  &:active {
    transform: scale(0.985);
  }
}

.mobile-setting-section.is-expanded .mobile-setting-trigger {
  border-color: color-mix(in srgb, var(--section-color) 48%, transparent);
  background: color-mix(in srgb, var(--section-color) 8%, var(--app-surface));
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.mobile-setting-icon {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: color-mix(in srgb, var(--section-color) 14%, transparent);
  color: var(--section-color);
  font-size: 18px;
}

.mobile-setting-title {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;

  strong {
    font-size: 14px;
    line-height: 1.2;
    color: var(--app-text);
  }

  small {
    font-size: 11px;
    line-height: 1.25;
    color: var(--app-text-secondary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.mobile-setting-arrow {
  justify-self: end;
  color: var(--app-text-secondary);
  transition: transform 0.24s cubic-bezier(0.2, 0.8, 0.2, 1), color 0.2s ease;
}

.mobile-setting-section.is-expanded .mobile-setting-arrow {
  color: var(--section-color);
  transform: rotate(180deg);
}

.mobile-setting-body-shell {
  display: grid;
  grid-template-rows: 0fr;
  opacity: 0;
  transform: translate3d(0, -4px, 0);
  pointer-events: none;
  overflow: hidden;
  contain: layout paint;
  transition:
    grid-template-rows 0.28s cubic-bezier(0.2, 0.8, 0.2, 1),
    opacity 0.18s ease,
    transform 0.24s cubic-bezier(0.2, 0.8, 0.2, 1),
    margin-top 0.2s ease;

  &.is-expanded {
    grid-template-rows: 1fr;
    opacity: 1;
    transform: translate3d(0, 0, 0);
    pointer-events: auto;
    margin-top: 8px;
  }
}

.mobile-setting-body {
  min-height: 0;
  padding: 14px;
  border: 1px solid var(--app-border);
  border-radius: 18px;
  background: var(--app-surface);
  color: var(--app-text);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
  overflow: hidden;
  contain: paint;
}

@media (prefers-reduced-motion: reduce) {
  .mobile-setting-trigger,
  .mobile-setting-arrow,
  .mobile-setting-body-shell {
    transition: none;
    transform: none;
  }
}
</style>
