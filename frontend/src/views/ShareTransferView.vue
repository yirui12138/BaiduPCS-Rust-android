<!--
SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
SPDX-License-Identifier: Apache-2.0

This file is part of the Android port in this repository.
Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
for Android integration, mobile UX, and distribution compliance.
See the repository LICENSE and NOTICE files for details.
-->

<template>
  <div class="share-transfer-container" :class="{ 'is-mobile': isMobile }">
    <header class="share-transfer-header">
      <div>
        <p class="eyebrow">SHARE CENTER</p>
        <h2 v-if="!isMobile">分享与转存</h2>
        <p class="subtitle">统一管理分享链接转存任务和自己创建的分享记录</p>
      </div>
    </header>

    <el-tabs
        v-model="activeTab"
        class="share-transfer-tabs"
        stretch
        @tab-change="syncTabToRoute"
    >
      <el-tab-pane label="转存任务" name="transfers">
        <TransfersView embedded />
      </el-tab-pane>
      <el-tab-pane label="我的分享" name="shares">
        <SharesView embedded />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useIsMobile } from '@/utils/responsive'
import TransfersView from '@/views/TransfersView.vue'
import SharesView from '@/views/SharesView.vue'

type ShareTransferTab = 'transfers' | 'shares'

const route = useRoute()
const router = useRouter()
const isMobile = useIsMobile()

function normalizeTab(tab: unknown): ShareTransferTab {
  return tab === 'shares' ? 'shares' : 'transfers'
}

const activeTab = ref<ShareTransferTab>(normalizeTab(route.query.tab))

function syncTabToRoute() {
  const nextQuery = { ...route.query, tab: activeTab.value }
  router.replace({ path: '/share-transfer', query: nextQuery })
}

watch(
  () => route.query.tab,
  (tab) => {
    activeTab.value = normalizeTab(tab)
  },
)
</script>

<style scoped lang="scss">
.share-transfer-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--app-bg);
  color: var(--app-text);
  overflow: hidden;
}

.share-transfer-header {
  flex-shrink: 0;
  padding: 18px 22px 10px;
  border-bottom: 1px solid var(--app-border);
  background: var(--app-surface);

  h2 {
    margin: 2px 0 4px;
    font-size: 22px;
    color: var(--app-text);
  }
}

.eyebrow {
  margin: 0;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.1em;
  color: var(--app-accent);
}

.subtitle {
  margin: 0;
  color: var(--app-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.share-transfer-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--app-bg);

  :deep(.el-tabs__header) {
    flex-shrink: 0;
    margin: 0;
    padding: 8px 14px 0;
    background: var(--app-surface);
  }

  :deep(.el-tabs__nav-wrap::after) {
    background: var(--app-border);
  }

  :deep(.el-tabs__item) {
    color: var(--app-text-secondary);
    font-weight: 700;

    &.is-active {
      color: var(--app-accent);
    }
  }

  :deep(.el-tabs__active-bar) {
    background: var(--app-accent);
  }

  :deep(.el-tabs__content) {
    flex: 1;
    min-height: 0;
    overflow: auto;
    padding: 12px 14px 24px;
  }

  :deep(.el-tab-pane) {
    min-height: 100%;
    overflow: visible;
  }
}

.is-mobile {
  .share-transfer-header {
    padding: 12px 14px 8px;

    .subtitle {
      font-size: 12px;
    }
  }

  .share-transfer-tabs {
    :deep(.el-tabs__header) {
      padding: 8px 10px 0;
    }

    :deep(.el-tabs__content) {
      padding: 10px 10px 18px;
    }
  }
}
</style>
