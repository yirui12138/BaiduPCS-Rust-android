<!--
SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
SPDX-License-Identifier: Apache-2.0

This file is part of the Android port in this repository.
Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
for Android integration, mobile UX, and distribution compliance.
See the repository LICENSE and NOTICE files for details.
-->

<template>
  <div
    class="mobile-share-card-shell"
    :class="{ 'is-expanded': expanded }"
    :aria-hidden="!expanded"
    :inert="!expanded ? true : undefined"
  >
    <section class="mobile-share-transfer-card" aria-label="分享转存">
      <div class="card-header">
        <div>
          <strong>分享转存</strong>
          <p>粘贴分享链接，转存到自己的网盘</p>
        </div>
        <el-button circle text size="small" @click="closePanel">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>

      <el-alert
        v-if="detectedShare && form.source === 'clipboard'"
        title="已从剪贴板识别到分享链接"
        type="success"
        :closable="false"
        show-icon
        class="compact-alert"
      />

      <div class="field-stack">
        <label>分享链接</label>
        <el-input
          v-model="form.shareUrl"
          placeholder="粘贴 pan.baidu.com 分享链接"
          clearable
          @paste="handlePaste"
          @blur="parseManualInput"
        />
      </div>

      <div class="field-grid">
        <div class="field-stack">
          <label>提取码</label>
          <el-input
            v-model="form.password"
            placeholder="可选"
            maxlength="4"
            clearable
          />
        </div>
        <div class="field-stack switch-field">
          <label>转存后下载</label>
          <el-switch v-model="form.autoDownload" />
        </div>
      </div>

      <div class="field-stack">
        <label>保存到网盘</label>
        <NetdiskPathSelector
          v-model="form.savePath"
          v-model:fs-id="form.saveFsId"
        />
      </div>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        show-icon
        class="compact-alert"
      />

      <div v-if="selectingFiles" class="file-select-card">
        <div class="select-card-header">
          <el-button link type="primary" @click="returnToAllTransfer">返回全部转存</el-button>
          <span>{{ selectedFsIds.length }} 项已选</span>
        </div>
        <ShareFileSelector
          :files="previewFiles"
          :loading="previewing"
          :share-info="shareInfo"
          :share-url="form.shareUrl"
          :share-password="form.password || undefined"
          @update:selected-fs-ids="handleSelectedFsIdsChange"
          @update:selected-files="handleSelectedFilesChange"
        />
      </div>

      <div class="panel-actions">
        <el-button
          plain
          :loading="previewing"
          :disabled="submitting || !form.shareUrl"
          @click="handleSelectFiles"
        >
          选择文件
        </el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="previewing || !form.shareUrl || (selectingFiles && selectedFsIds.length === 0)"
          @click="handleCreateTransfer"
        >
          开始转存
        </el-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Close } from '@element-plus/icons-vue'
import NetdiskPathSelector from '@/components/NetdiskPathSelector.vue'
import ShareFileSelector from '@/components/ShareFileSelector.vue'
import {
  createTransfer,
  previewShareFiles,
  TransferErrorCodes,
  type CreateTransferRequest,
  type PreviewShareInfo,
  type SharedFileInfo,
} from '@/api/transfer'
import { getConfig, getTransferConfig } from '@/api/config'
import { readClipboardTextInAndroid } from '@/utils/androidBridge'
import { parseBaiduShareText, type ParsedShareLink } from '@/utils/shareLinkParser'

const props = withDefaults(defineProps<{
  currentPath?: string
}>(), {
  currentPath: '/',
})

const emit = defineEmits<{
  success: [taskId: string]
  'state-change': [state: { expanded: boolean; detected: boolean }]
}>()

const expanded = ref(false)
const detectingEnabled = ref(true)
const detectedShare = ref<ParsedShareLink | null>(null)
const lastClipboardShareUrl = ref('')
const previewing = ref(false)
const submitting = ref(false)
const selectingFiles = ref(false)
const previewFiles = ref<SharedFileInfo[]>([])
const selectedFsIds = ref<number[]>([])
const selectedFiles = ref<SharedFileInfo[]>([])
const shareInfo = ref<PreviewShareInfo | null>(null)
const errorMessage = ref('')

const form = reactive({
  shareUrl: '',
  password: '',
  source: 'manual' as ParsedShareLink['source'],
  savePath: '/',
  saveFsId: 0,
  autoDownload: false,
})

const hasDetectedShare = computed(() => Boolean(detectedShare.value))
const isExpanded = computed(() => expanded.value)

watch(
  [expanded, detectedShare],
  () => {
    emit('state-change', {
      expanded: expanded.value,
      detected: Boolean(detectedShare.value),
    })
  },
  { immediate: true },
)

watch(
  () => props.currentPath,
  (path) => {
    if (!expanded.value) {
      form.savePath = normalizeSavePath(path)
      form.saveFsId = 0
    }
  },
)

function togglePanel() {
  if (!expanded.value && detectedShare.value) {
    applyParsedShare(detectedShare.value)
  }
  expanded.value = !expanded.value
}

function openPanel() {
  if (detectedShare.value) {
    applyParsedShare(detectedShare.value)
  }
  expanded.value = true
}

function closePanel() {
  expanded.value = false
}

function applyParsedShare(parsed: ParsedShareLink) {
  form.shareUrl = parsed.shareUrl
  form.password = parsed.password || form.password
  form.source = parsed.source
  errorMessage.value = ''
}

function parseManualInput() {
  const parsed = parseBaiduShareText(form.shareUrl, 'manual')
  if (!parsed) return
  form.shareUrl = parsed.shareUrl
  if (parsed.password && !form.password) {
    form.password = parsed.password
  }
}

function handlePaste(event: ClipboardEvent) {
  const text = event.clipboardData?.getData('text') || ''
  const parsed = parseBaiduShareText(text, 'manual')
  if (!parsed) return
  event.preventDefault()
  applyParsedShare(parsed)
}

function normalizeSavePath(path: string | undefined): string {
  const value = (path || '/').trim()
  return value.startsWith('/') ? value : `/${value}`
}

async function loadDefaults(updateForm = true) {
  try {
    const [appConfig, transferConfig] = await Promise.all([
      getConfig(),
      getTransferConfig(),
    ])
    detectingEnabled.value = appConfig.mobile?.clipboard_share_detection_enabled !== false
    if (!detectingEnabled.value && detectedShare.value?.source === 'clipboard') {
      detectedShare.value = null
    }
    if (updateForm && !expanded.value) {
      form.autoDownload = transferConfig.default_behavior === 'transfer_and_download'
      form.savePath = normalizeSavePath(props.currentPath || transferConfig.recent_save_path || '/')
      form.saveFsId = 0
    }
  } catch (error) {
    console.warn('[MobileShareTransferCard] failed to load defaults', error)
  }
}

async function detectClipboardShare() {
  if (!detectingEnabled.value) return

  const clipboardText = readClipboardTextInAndroid()
  const parsed = parseBaiduShareText(clipboardText, 'clipboard')
  if (!parsed) return
  if (parsed.shareUrl === lastClipboardShareUrl.value) return

  lastClipboardShareUrl.value = parsed.shareUrl
  detectedShare.value = parsed
}

async function refreshDetectionSettingsAndClipboard() {
  await loadDefaults(!expanded.value)
  await detectClipboardShare()
}

async function handleSelectFiles() {
  if (!form.shareUrl) return
  previewing.value = true
  errorMessage.value = ''
  try {
    const response = await previewShareFiles({
      share_url: form.shareUrl.trim(),
      password: form.password || undefined,
    })
    previewFiles.value = response.files
    shareInfo.value = response.share_info || null
    selectingFiles.value = true
  } catch (error: any) {
    handleTransferError(error, '预览失败，请稍后重试')
  } finally {
    previewing.value = false
  }
}

function handleSelectedFsIdsChange(fsIds: number[]) {
  selectedFsIds.value = fsIds
}

function handleSelectedFilesChange(files: SharedFileInfo[]) {
  selectedFiles.value = files
}

function returnToAllTransfer() {
  selectingFiles.value = false
  selectedFsIds.value = []
  selectedFiles.value = []
}

async function handleCreateTransfer() {
  if (!form.shareUrl) return
  submitting.value = true
  errorMessage.value = ''

  try {
    const request: CreateTransferRequest = {
      share_url: form.shareUrl.trim(),
      password: form.password || undefined,
      save_path: form.savePath,
      save_fs_id: form.saveFsId,
      auto_download: form.autoDownload,
      selected_fs_ids: selectingFiles.value && selectedFsIds.value.length > 0 ? selectedFsIds.value : undefined,
      selected_files: selectingFiles.value && selectedFiles.value.length > 0 ? selectedFiles.value : undefined,
    }
    const response = await createTransfer(request)
    if (response.task_id) {
      ElMessage.success('转存任务已创建，可在转存管理查看进度')
      emit('success', response.task_id)
      detectedShare.value = null
      resetPanel()
      expanded.value = false
    }
  } catch (error: any) {
    handleTransferError(error, '转存失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

function resetPanel() {
  form.shareUrl = ''
  form.password = ''
  form.source = 'manual'
  form.savePath = normalizeSavePath(props.currentPath)
  form.saveFsId = 0
  selectingFiles.value = false
  previewFiles.value = []
  selectedFsIds.value = []
  selectedFiles.value = []
  shareInfo.value = null
  errorMessage.value = ''
}

function handleTransferError(error: any, fallback: string) {
  switch (error?.code) {
    case TransferErrorCodes.NEED_PASSWORD:
      errorMessage.value = '该分享需要提取码，请输入后重试'
      break
    case TransferErrorCodes.INVALID_PASSWORD:
      errorMessage.value = '提取码错误，请检查后重新输入'
      break
    case TransferErrorCodes.SHARE_EXPIRED:
      errorMessage.value = '分享链接已失效'
      break
    case TransferErrorCodes.SHARE_NOT_FOUND:
      errorMessage.value = '分享不存在或已被取消'
      break
    default:
      errorMessage.value = error?.message || fallback
      break
  }
}

function handleVisibilityChange() {
  if (document.visibilityState === 'visible') {
    refreshDetectionSettingsAndClipboard()
  }
}

onMounted(() => {
  refreshDetectionSettingsAndClipboard()
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('focus', refreshDetectionSettingsAndClipboard)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('focus', refreshDetectionSettingsAndClipboard)
})

defineExpose({
  togglePanel,
  openPanel,
  closePanel,
  hasDetectedShare,
  isExpanded,
})
</script>

<style scoped lang="scss">
.mobile-share-card-shell {
  display: grid;
  grid-template-rows: 0fr;
  margin: 0 12px;
  overflow: hidden;
  opacity: 0;
  transform: translate3d(0, -6px, 0);
  pointer-events: none;
  contain: layout paint;
  transition:
    grid-template-rows 0.28s cubic-bezier(0.2, 0.8, 0.2, 1),
    opacity 0.18s ease,
    transform 0.24s cubic-bezier(0.2, 0.8, 0.2, 1),
    margin-bottom 0.24s ease;
  will-change: grid-template-rows, opacity, transform;

  &.is-expanded {
    grid-template-rows: 1fr;
    margin-bottom: 10px;
    opacity: 1;
    transform: translate3d(0, 0, 0);
    pointer-events: auto;
  }
}

.mobile-share-transfer-card {
  min-height: 0;
  padding: 12px;
  border: 1px solid var(--app-border);
  border-radius: 18px;
  background: var(--app-surface);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
  color: var(--app-text);
  contain: paint;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;

  strong {
    display: block;
    font-size: 16px;
    line-height: 1.25;
  }

  p {
    margin: 4px 0 0;
    color: var(--app-text-secondary);
    font-size: 12px;
    line-height: 1.4;
  }
}

.field-stack {
  display: grid;
  gap: 6px;
  margin-bottom: 10px;

  label {
    color: var(--app-text-secondary);
    font-size: 12px;
    font-weight: 700;
  }
}

.field-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: end;
}

.switch-field {
  min-width: 104px;
  justify-items: start;
}

.compact-alert {
  margin-bottom: 10px;
}

.file-select-card {
  max-height: 240px;
  overflow: auto;
  margin: 10px 0;
  padding: 10px;
  border: 1px solid var(--app-border);
  border-radius: 14px;
  background: var(--app-surface-strong);
}

.select-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: var(--app-text-secondary);
  font-size: 12px;
}

.panel-actions {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 10px;
  margin-top: 12px;

  .el-button {
    width: 100%;
  }
}

@media (max-width: 360px) {
  .mobile-share-card-shell {
    margin-inline: 10px;
  }

  .mobile-share-transfer-card {
    padding: 10px;
  }

  .field-grid {
    grid-template-columns: 1fr;
  }

  .panel-actions {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .mobile-share-card-shell {
    transition: none;
    transform: none;
  }
}
</style>
