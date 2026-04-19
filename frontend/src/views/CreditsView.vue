<!--
SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
SPDX-License-Identifier: Apache-2.0

This file is part of the Android port in this repository.
Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
for Android integration, mobile UX, and distribution compliance.
See the repository LICENSE and NOTICE files for details.
-->

<template>
  <div class="credits-container">
    <section class="hero-card">
      <p class="eyebrow">Open Source</p>
      <h1>{{ APP_LEGAL_PAGE_NAME }}</h1>
      <p class="summary">
        {{ APP_DISPLAY_NAME }} 基于上游开源项目 <strong>{{ UPSTREAM_PROJECT_NAME }}</strong>
        进行 Android 本地化封装、移动端适配和运行时集成。这里集中展示版权、许可证、NOTICE
        和实际进入 APK 的运行时第三方依赖清单。
      </p>
    </section>

    <el-card class="credits-card" shadow="never">
      <template #header>
        <div class="card-title">
          <el-icon><InfoFilled /></el-icon>
          <span>应用信息</span>
        </div>
      </template>

      <div class="info-list">
        <div class="info-row">
          <span class="label">应用名称</span>
          <span class="value">{{ APP_DISPLAY_NAME }}</span>
        </div>
        <div class="info-row">
          <span class="label">当前版本</span>
          <span class="value">v{{ appVersion }}</span>
        </div>
        <div class="info-row">
          <span class="label">移植说明</span>
          <span class="value">Android 本地化封装、UI 适配与系统能力集成版</span>
        </div>
        <div class="info-row">
          <span class="label">非官方说明</span>
          <span class="value">{{ APP_NON_OFFICIAL_NOTICE }}</span>
        </div>
      </div>
    </el-card>

    <el-card class="credits-card" shadow="never">
      <template #header>
        <div class="card-title">
          <el-icon><Link /></el-icon>
          <span>上游署名与来源</span>
        </div>
      </template>

      <div class="source-block">
        <p>上游项目：<strong>{{ UPSTREAM_PROJECT_NAME }}</strong></p>
        <p>原作者：<strong>{{ UPSTREAM_AUTHOR }}</strong></p>
        <p>引用版本：<strong>{{ UPSTREAM_VERSION }}</strong></p>
        <p>原始许可证：<strong>Apache License 2.0</strong></p>
        <el-link
          class="source-link"
          :href="UPSTREAM_RELEASE_URL"
          target="_blank"
          rel="noopener noreferrer"
          type="primary"
        >
          {{ UPSTREAM_RELEASE_URL }}
        </el-link>
      </div>
    </el-card>

    <el-card class="credits-card" shadow="never">
      <template #header>
        <div class="card-title">
          <el-icon><Memo /></el-icon>
          <span>移植说明与 NOTICE</span>
        </div>
      </template>

      <div class="compliance-note">
        <p>
          本移植版保留上游项目的作者署名、来源链接和 Apache License 2.0 许可证文本；
          与上游直接相关的版权、归属和许可证条款，仍以上游仓库发布内容为准。
        </p>
        <p>
          原项目相关的版权与贡献者署名，仍归 <strong>{{ UPSTREAM_AUTHOR }}</strong> 及其贡献者所有；
          本移植版仅在原许可范围内进行 Android 封装、界面适配和系统集成。
        </p>
        <p>
          应用内提供的 NOTICE 用于说明本 Android 移植版的封装性质、修改方向和非官方发布关系，
          不改变上游项目原有的版权与许可归属。
        </p>
      </div>

      <pre v-if="noticeText" class="legal-text">{{ noticeText }}</pre>
      <el-skeleton v-else :rows="6" animated />
    </el-card>

    <el-card class="credits-card" shadow="never">
      <template #header>
        <div class="card-title">
          <el-icon><CollectionTag /></el-icon>
          <span>第三方运行时依赖</span>
        </div>
      </template>

      <div class="packages-meta">
        <span>总计 {{ packageCount }} 项</span>
        <span v-if="generatedAtLabel">生成时间：{{ generatedAtLabel }}</span>
      </div>

      <el-tabs v-model="activeSourceTab" stretch>
        <el-tab-pane
          v-for="tab in sourceTabs"
          :key="tab.key"
          :label="`${tab.label} (${packagesBySource[tab.key].length})`"
          :name="tab.key"
        >
          <el-empty
            v-if="packagesBySource[tab.key].length === 0 && !loadingPackages"
            description="当前没有可显示的运行时依赖"
          />
          <el-skeleton v-else-if="loadingPackages" :rows="5" animated />
          <div v-else class="package-list">
            <article
              v-for="entry in packagesBySource[tab.key]"
              :key="`${entry.source}-${entry.name}-${entry.version}`"
              class="package-item"
            >
              <div class="package-main">
                <div class="package-name-row">
                  <h3>{{ entry.name }}</h3>
                  <el-tag size="small" effect="plain">{{ entry.license_expression }}</el-tag>
                </div>
                <p class="package-meta-text">版本 {{ entry.version }}</p>
                <p v-if="entry.homepage" class="package-meta-text">
                  主页：
                  <el-link :href="entry.homepage" target="_blank" rel="noopener noreferrer" type="primary">
                    {{ entry.homepage }}
                  </el-link>
                </p>
              </div>
              <div class="package-actions">
                <el-button text type="primary" @click="openLegalText(entry, 'license')">
                  查看许可证
                </el-button>
                <el-button
                  v-if="entry.notice_path"
                  text
                  type="primary"
                  @click="openLegalText(entry, 'notice')"
                >
                  查看 NOTICE
                </el-button>
              </div>
            </article>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-card class="credits-card license-card" shadow="never">
      <template #header>
        <div class="card-title">
          <el-icon><Reading /></el-icon>
          <span>Apache License 2.0 全文</span>
        </div>
      </template>

      <pre v-if="upstreamLicenseText" class="legal-text">{{ upstreamLicenseText }}</pre>
      <el-skeleton v-else :rows="10" animated />
    </el-card>

    <el-dialog
      v-model="legalDialogVisible"
      :title="legalDialogTitle"
      :width="isMobile ? '92%' : '860px'"
      class="legal-dialog"
      destroy-on-close
    >
      <pre v-if="activeLegalText" class="dialog-legal-text">{{ activeLegalText }}</pre>
      <el-skeleton v-else :rows="8" animated />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useIsMobile } from '@/utils/responsive'
import {
  APP_DISPLAY_NAME,
  APP_LEGAL_PAGE_NAME,
  APP_NON_OFFICIAL_NOTICE,
  UPSTREAM_AUTHOR,
  UPSTREAM_PROJECT_NAME,
  UPSTREAM_RELEASE_URL,
  UPSTREAM_VERSION,
  getAppVersion,
} from '@/constants/appInfo'
import {
  CollectionTag,
  InfoFilled,
  Link,
  Memo,
  Reading,
} from '@element-plus/icons-vue'

type LegalSource = 'rust' | 'web' | 'android'
type LegalTextKind = 'license' | 'notice'

interface ThirdPartyPackageEntry {
  source: LegalSource
  name: string
  version: string
  license_expression: string
  license_path: string
  notice_path: string | null
  homepage?: string | null
}

interface ThirdPartyIndex {
  generatedAt: string
  appName: string
  packages: ThirdPartyPackageEntry[]
}

const isMobile = useIsMobile()
const appVersion = getAppVersion()
const activeSourceTab = ref<LegalSource>('rust')
const upstreamLicenseText = ref('')
const noticeText = ref('')
const loadingPackages = ref(true)
const thirdPartyPackages = ref<ThirdPartyPackageEntry[]>([])
const legalDialogVisible = ref(false)
const legalDialogTitle = ref('')
const activeLegalText = ref('')

const sourceTabs = [
  { key: 'rust' as const, label: 'Rust' },
  { key: 'web' as const, label: 'Web' },
  { key: 'android' as const, label: 'Android' },
]

const packagesBySource = computed<Record<LegalSource, ThirdPartyPackageEntry[]>>(() => ({
  rust: thirdPartyPackages.value.filter((entry) => entry.source === 'rust'),
  web: thirdPartyPackages.value.filter((entry) => entry.source === 'web'),
  android: thirdPartyPackages.value.filter((entry) => entry.source === 'android'),
}))

const packageCount = computed(() => thirdPartyPackages.value.length)
const generatedAtLabel = ref('')

async function fetchText(path: string): Promise<string> {
  const response = await fetch(path, { cache: 'no-store' })
  if (!response.ok) {
    throw new Error(`Failed to fetch ${path}`)
  }
  return response.text()
}

async function openLegalText(entry: ThirdPartyPackageEntry, kind: LegalTextKind) {
  const targetPath = kind === 'license' ? entry.license_path : entry.notice_path
  if (!targetPath) {
    return
  }

  legalDialogTitle.value = kind === 'license'
    ? `${entry.name} ${entry.version} 许可证`
    : `${entry.name} ${entry.version} NOTICE`
  legalDialogVisible.value = true
  activeLegalText.value = ''

  try {
    activeLegalText.value = await fetchText(targetPath)
  } catch (error) {
    console.error('Failed to load legal text', error)
    activeLegalText.value = '未能加载法律文本，请稍后重试。'
  }
}

async function loadThirdPartyIndex() {
  loadingPackages.value = true
  try {
    const response = await fetch('/open-source/third-party-index.json', {
      cache: 'no-store',
    })
    if (!response.ok) {
      throw new Error('third-party index fetch failed')
    }

    const index = (await response.json()) as ThirdPartyIndex
    thirdPartyPackages.value = index.packages ?? []
    generatedAtLabel.value = index.generatedAt
      ? new Date(index.generatedAt).toLocaleString('zh-CN')
      : ''
  } catch (error) {
    console.error('Failed to load third-party index', error)
    thirdPartyPackages.value = []
    generatedAtLabel.value = ''
  } finally {
    loadingPackages.value = false
  }
}

onMounted(async () => {
  try {
    upstreamLicenseText.value = await fetchText('/open-source/LICENSE.txt')
  } catch (error) {
    console.error('Failed to load upstream license text', error)
  }

  try {
    noticeText.value = await fetchText('/open-source/NOTICE.txt')
  } catch (error) {
    console.error('Failed to load notice text', error)
  }

  await loadThirdPartyIndex()
})
</script>

<style scoped lang="scss">
.credits-container {
  height: 100%;
  min-height: 0;
  overflow: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: transparent;
}

.hero-card,
.credits-card {
  border: 1px solid var(--app-border);
  border-radius: 24px;
  background: var(--app-surface);
  box-shadow: var(--app-shadow);
}

.hero-card {
  padding: 24px;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--app-accent);
}

h1 {
  margin: 0 0 10px;
  font-size: 28px;
  color: var(--app-text);
}

.summary {
  margin: 0;
  line-height: 1.8;
  color: var(--app-text-secondary);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--app-text);
  font-weight: 600;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--app-border);

  &:last-child {
    padding-bottom: 0;
    border-bottom: none;
  }
}

.label {
  color: var(--app-text-secondary);
  font-size: 14px;
}

.value {
  color: var(--app-text);
  font-size: 14px;
  font-weight: 500;
  text-align: right;
}

.source-block,
.compliance-note {
  display: flex;
  flex-direction: column;
  gap: 10px;
  color: var(--app-text);

  p {
    margin: 0;
    line-height: 1.75;
  }
}

.source-link {
  width: fit-content;
  max-width: 100%;
  word-break: break-all;
}

.packages-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  color: var(--app-text-secondary);
  font-size: 13px;
  flex-wrap: wrap;
}

.package-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.package-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border-radius: 18px;
  background: var(--app-surface-muted);
  border: 1px solid var(--app-border);
}

.package-main {
  min-width: 0;
}

.package-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;

  h3 {
    margin: 0;
    font-size: 16px;
    color: var(--app-text);
  }
}

.package-meta-text {
  margin: 8px 0 0;
  line-height: 1.65;
  color: var(--app-text-secondary);
  font-size: 13px;
  word-break: break-all;
}

.package-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 4px;
  flex-shrink: 0;
}

.license-card {
  min-height: 320px;
}

.legal-text,
.dialog-legal-text {
  margin: 0;
  padding: 16px;
  border-radius: 18px;
  background: var(--app-surface-muted);
  border: 1px solid var(--app-border);
  color: var(--app-text);
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.72;
  font-family: 'Cascadia Mono', 'Consolas', monospace;
}

.dialog-legal-text {
  max-height: 65vh;
  overflow: auto;
}

html.dark {
  .hero-card,
  .credits-card {
    box-shadow: none;
  }
}

@media (max-width: 767px) {
  .credits-container {
    padding: 12px;
    gap: 12px;
  }

  .hero-card {
    padding: 18px;
  }

  h1 {
    font-size: 22px;
  }

  .info-row,
  .package-item {
    flex-direction: column;
  }

  .value,
  .package-actions {
    text-align: left;
    align-items: flex-start;
  }

  .legal-text,
  .dialog-legal-text {
    padding: 14px;
    font-size: 11px;
  }
}
</style>
