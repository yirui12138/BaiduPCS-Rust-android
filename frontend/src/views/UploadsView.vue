<!--
SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
SPDX-License-Identifier: Apache-2.0

This file is part of the Android port in this repository.
Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
for Android integration, mobile UX, and distribution compliance.
See the repository LICENSE and NOTICE files for details.
-->

<template>
  <div class="uploads-container" :class="{ 'is-mobile': isMobile }">
    <section class="status-card">
      <div class="status-head">
        <div class="status-copy">
          <p class="eyebrow">Upload Center</p>
          <h2>上传管理</h2>
          <p class="status-description">
            {{ androidImportEnabled
              ? '上传统一从这里发起。安卓会先导入到 App 专属空间，再由你确认后自动加入上传队列。'
              : '上传统一从这里发起。选择本地文件或文件夹后，会直接加入上传队列。' }}
          </p>
        </div>
        <el-tag :type="activeCountType" size="large">{{ activeCount }} 个任务进行中</el-tag>
      </div>

      <div class="status-body">
        <div class="target-panel">
          <span class="field-label">上传到网盘目录</span>
          <NetdiskPathSelector v-model="targetRemotePath" v-model:fs-id="targetRemoteFsId" />
          <p class="field-hint">当前目标：{{ targetRemotePath || '/' }}</p>
        </div>

        <div class="status-actions">
          <el-button @click="refreshTasks" :circle="isMobile">
            <el-icon><Refresh /></el-icon>
            <span v-if="!isMobile">刷新</span>
          </el-button>
          <el-dropdown trigger="click" @command="handleBatchCommand">
            <el-button :circle="isMobile">
              <el-icon v-if="isMobile"><ArrowDown /></el-icon>
              <template v-else>
                批量操作
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </template>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="pause" :disabled="activeCount === 0">
                  <el-icon><VideoPause /></el-icon>
                  全部暂停 ({{ activeCount }})
                </el-dropdown-item>
                <el-dropdown-item command="resume" :disabled="pausedCount === 0">
                  <el-icon><VideoPlay /></el-icon>
                  全部继续 ({{ pausedCount }})
                </el-dropdown-item>
                <el-dropdown-item command="clearCompleted" :disabled="completedCount === 0" divided>
                  <el-icon><Delete /></el-icon>
                  清除已完成 ({{ completedCount }})
                </el-dropdown-item>
                <el-dropdown-item command="clearFailed" :disabled="failedCount === 0">
                  <el-icon><Delete /></el-icon>
                  清除失败 ({{ failedCount }})
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button v-if="!isMobile" type="primary" @click="openUploadChooser">
            <el-icon><Upload /></el-icon>
            上传
          </el-button>
        </div>
      </div>
    </section>

    <div class="task-container">
      <el-empty v-if="!loading && uploadItems.length === 0" description="暂无上传任务" class="empty-state">
        <template #image>
          <el-icon :size="80" color="var(--app-accent)"><Upload /></el-icon>
        </template>
        <template #description>
          <div class="empty-copy">
            <p>上传入口已经统一收口到本页。</p>
            <p>{{ isMobile ? '点击右下角“上传”即可开始。' : '点击右上角“上传”即可开始。' }}</p>
          </div>
        </template>
      </el-empty>

      <div v-else class="task-list">
        <el-card
          v-for="item in uploadItems"
          :key="item.id"
          class="task-card"
          :class="{ 'task-active': item.status === 'uploading' || item.status === 'encrypting' }"
          shadow="hover"
        >
          <div class="task-header">
            <div class="task-info">
              <div class="task-title">
                <el-icon :size="20" class="file-icon"><Upload /></el-icon>
                <span class="filename">{{ getFilename(item.local_path) }}</span>
                <el-tag :type="getStatusType(item.status)" size="small">{{ getStatusText(item.status) }}</el-tag>
              </div>
              <div class="task-path">本地: {{ item.local_path }} -> 网盘: {{ item.remote_path }}</div>
            </div>

            <div class="task-actions">
              <el-button v-if="item.status === 'uploading'" size="small" @click="handlePause(item)">
                <el-icon><VideoPause /></el-icon>
                暂停
              </el-button>
              <el-button v-if="item.status === 'paused'" size="small" type="primary" @click="handleResume(item)">
                <el-icon><VideoPlay /></el-icon>
                继续
              </el-button>
              <el-button v-if="item.status === 'failed'" size="small" type="warning" @click="handleResume(item)">
                <el-icon><RefreshRight /></el-icon>
                重试
              </el-button>
              <el-button size="small" type="danger" @click="handleDelete(item)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </div>

          <div v-if="item.status === 'encrypting'" class="encrypt-progress">
            <div class="encrypt-header">
              <el-icon class="encrypt-icon"><Lock /></el-icon>
              <span>正在加密文件...</span>
            </div>
            <el-progress :percentage="item.encrypt_progress || 0" :stroke-width="6" status="warning" />
          </div>

          <div v-else class="task-progress">
            <el-progress :percentage="calculateProgress(item)" :status="getProgressStatus(item.status)" :stroke-width="8" />
          </div>

          <div class="task-stats">
            <div class="stat-item"><span class="stat-label">已上传</span><span class="stat-value">{{ formatFileSize(item.uploaded_size) }}</span></div>
            <div class="stat-item"><span class="stat-label">总大小</span><span class="stat-value">{{ formatFileSize(item.total_size) }}</span></div>
            <div class="stat-item" v-if="item.status === 'uploading'"><span class="stat-label">速度</span><span class="stat-value speed">{{ formatSpeed(item.speed) }}</span></div>
            <div class="stat-item" v-if="item.status === 'uploading'"><span class="stat-label">剩余时间</span><span class="stat-value">{{ formatETA(calculateETA(item)) }}</span></div>
            <div class="stat-item" v-if="item.error"><span class="stat-label error">错误</span><span class="stat-value error">{{ item.error }}</span></div>
          </div>
        </el-card>
      </div>
    </div>

    <FilePickerModal
      v-model="showDesktopFilePicker"
      :select-type="desktopPickerSelectType"
      :title="desktopPickerSelectType === 'directory' ? '选择上传文件夹' : '选择上传文件'"
      :confirm-text="'加入上传队列'"
      :multiple="true"
      :initial-path="uploadPickerInitialPath"
      :show-encryption="hasEncryptionKey"
      :show-conflict-strategy="true"
      :default-upload-conflict-strategy="uploadConflictStrategy"
      @select="handleFilePickerSelect"
      @select-multiple="handleFilePickerMultiSelect"
    />

    <el-button v-if="isMobile" class="mobile-upload-fab" type="primary" round @click="openUploadChooser">
      <el-icon><Upload /></el-icon>
      上传
    </el-button>

    <el-drawer
      v-if="isMobile"
      v-model="uploadChooserVisible"
      :with-header="false"
      direction="btt"
      size="auto"
      class="upload-drawer"
    >
      <div class="sheet-card">
        <div class="sheet-handle" />
        <p class="eyebrow">Upload</p>
        <h4>选择上传内容</h4>
        <p class="sheet-copy">{{ chooserDescription }}</p>
        <div class="sheet-actions">
          <el-button type="primary" size="large" @click="handleChooseFiles">
            <el-icon><DocumentAdd /></el-icon>
            选择文件
          </el-button>
          <el-button size="large" @click="handleChooseFolders">
            <el-icon><FolderOpened /></el-icon>
            选择文件夹
          </el-button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-else v-model="uploadChooserVisible" width="420px" align-center class="upload-dialog">
      <div class="sheet-card dialog-card">
        <p class="eyebrow">Upload</p>
        <h4>选择上传内容</h4>
        <p class="sheet-copy">{{ chooserDescription }}</p>
        <div class="sheet-actions">
          <el-button type="primary" size="large" @click="handleChooseFiles">
            <el-icon><DocumentAdd /></el-icon>
            选择文件
          </el-button>
          <el-button size="large" @click="handleChooseFolders">
            <el-icon><FolderOpened /></el-icon>
            选择文件夹
          </el-button>
        </div>
      </div>
    </el-dialog>

    <el-drawer
      v-if="isMobile"
      v-model="confirmUploadVisible"
      :with-header="false"
      direction="btt"
      size="auto"
      class="upload-drawer"
    >
      <div class="sheet-card confirm-card">
        <div class="sheet-handle" />
        <p class="eyebrow">Confirm Upload</p>
        <h4>确认上传</h4>
        <p class="sheet-copy">文件已经复制到 App 专属空间，确认后会自动创建上传任务。</p>
        <div class="summary-card">
          <div class="summary-head">
            <span>本次导入</span>
            <el-tag type="success" size="small">{{ pendingImportedEntries.length }} 项</el-tag>
          </div>
          <div class="summary-list">
            <div v-for="entry in previewImportedEntries" :key="entry.path" class="summary-item">
              <el-icon><FolderOpened v-if="entry.entryType === 'directory'" /><DocumentAdd v-else /></el-icon>
              <span>{{ entry.name }}</span>
            </div>
            <p v-if="pendingImportedEntries.length > previewImportedEntries.length" class="summary-more">
              还有 {{ pendingImportedEntries.length - previewImportedEntries.length }} 项会一起上传
            </p>
          </div>
        </div>
        <div class="confirm-fields">
          <div class="confirm-field">
            <span class="field-label">上传到网盘目录</span>
            <NetdiskPathSelector v-model="targetRemotePath" v-model:fs-id="targetRemoteFsId" />
          </div>
          <div class="confirm-inline">
            <div v-if="hasEncryptionKey" class="inline-option">
              <span>加密上传</span>
              <el-switch v-model="confirmEncryptEnabled" />
            </div>
            <div class="inline-option strategy-option">
              <span>冲突策略</span>
              <el-select v-model="confirmConflictStrategy" size="small">
                <el-option label="智能去重" value="smart_dedup" />
                <el-option label="自动重命名" value="auto_rename" />
                <el-option label="覆盖" value="overwrite" />
              </el-select>
            </div>
          </div>
        </div>
        <div class="confirm-actions">
          <el-button @click="cancelPendingUpload">取消</el-button>
          <el-button type="primary" :loading="confirmSubmitting" @click="confirmPendingUpload">确认上传</el-button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-else v-model="confirmUploadVisible" width="520px" align-center class="upload-dialog">
      <div class="sheet-card dialog-card confirm-card">
        <p class="eyebrow">Confirm Upload</p>
        <h4>确认上传</h4>
        <p class="sheet-copy">文件已经复制到 App 专属空间，确认后会自动创建上传任务。</p>
        <div class="summary-card">
          <div class="summary-head">
            <span>本次导入</span>
            <el-tag type="success" size="small">{{ pendingImportedEntries.length }} 项</el-tag>
          </div>
          <div class="summary-list">
            <div v-for="entry in previewImportedEntries" :key="entry.path" class="summary-item">
              <el-icon><FolderOpened v-if="entry.entryType === 'directory'" /><DocumentAdd v-else /></el-icon>
              <span>{{ entry.name }}</span>
            </div>
            <p v-if="pendingImportedEntries.length > previewImportedEntries.length" class="summary-more">
              还有 {{ pendingImportedEntries.length - previewImportedEntries.length }} 项会一起上传
            </p>
          </div>
        </div>
        <div class="confirm-fields">
          <div class="confirm-field">
            <span class="field-label">上传到网盘目录</span>
            <NetdiskPathSelector v-model="targetRemotePath" v-model:fs-id="targetRemoteFsId" />
          </div>
          <div class="confirm-inline">
            <div v-if="hasEncryptionKey" class="inline-option">
              <span>加密上传</span>
              <el-switch v-model="confirmEncryptEnabled" />
            </div>
            <div class="inline-option strategy-option">
              <span>冲突策略</span>
              <el-select v-model="confirmConflictStrategy" size="small">
                <el-option label="智能去重" value="smart_dedup" />
                <el-option label="自动重命名" value="auto_rename" />
                <el-option label="覆盖" value="overwrite" />
              </el-select>
            </div>
          </div>
        </div>
        <div class="confirm-actions">
          <el-button @click="cancelPendingUpload">取消</el-button>
          <el-button type="primary" :loading="confirmSubmitting" @click="confirmPendingUpload">确认上传</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FilePickerModal } from '@/components/FilePicker'
import NetdiskPathSelector from '@/components/NetdiskPathSelector.vue'
import { useIsMobile } from '@/utils/responsive'
import { getConfig, updateRecentDirDebounced, type UploadConfig } from '@/api/config'
import { getEncryptionStatus } from '@/api/autobackup'
import type { FileEntry } from '@/api/filesystem'
import {
  getAllUploads,
  pauseUpload,
  resumeUpload,
  deleteUpload,
  clearCompleted,
  clearFailed,
  batchPauseUploads,
  batchResumeUploads,
  createUpload,
  createFolderUpload,
  calculateProgress,
  calculateETA,
  formatFileSize,
  formatSpeed,
  formatETA,
  getStatusText,
  getStatusType,
  extractFilename,
  type UploadConflictStrategy,
  type UploadTask,
  type UploadTaskStatus,
} from '@/api/upload'
import {
  ANDROID_IMPORT_COMPLETE_EVENT,
  canImportFromAndroid,
  cleanupImportedPathsInAndroid,
  cleanupStaleImportsInAndroid,
  importFilesFromAndroid,
  importFolderFromAndroid,
  type AndroidImportCompleteDetail,
  type AndroidImportedEntry,
} from '@/utils/androidBridge'
import { getWebSocketClient, connectWebSocket, type ConnectionState } from '@/utils/websocket'
import { usePageVisibility } from '@/utils/pageVisibility'
import type { UploadEvent } from '@/types/events'
import {
  Refresh,
  Upload,
  VideoPause,
  VideoPlay,
  Delete,
  RefreshRight,
  Lock,
  ArrowDown,
  FolderOpened,
  DocumentAdd,
} from '@element-plus/icons-vue'

type PickerSelectType = 'file' | 'directory'

const isMobile = useIsMobile()
const isPageVisible = usePageVisibility()

const loading = ref(false)
const uploadItems = ref<UploadTask[]>([])
const uploadConfig = ref<UploadConfig | null>(null)
const hasEncryptionKey = ref(false)
const targetRemotePath = ref('/')
const targetRemoteFsId = ref(0)
const uploadConflictStrategy = ref<UploadConflictStrategy>('smart_dedup')
const confirmConflictStrategy = ref<UploadConflictStrategy>('smart_dedup')
const confirmEncryptEnabled = ref(false)
const showDesktopFilePicker = ref(false)
const desktopPickerSelectType = ref<PickerSelectType>('file')
const uploadChooserVisible = ref(false)
const confirmUploadVisible = ref(false)
const confirmSubmitting = ref(false)
const pendingImportedEntries = ref<FileEntry[]>([])
const wsConnected = ref(false)

let refreshTimer: number | null = null
let unsubscribeUpload: (() => void) | null = null
let unsubscribeConnectionState: (() => void) | null = null

const FALLBACK_REFRESH_MS = 2500
const IMPORT_CLEANUP_STORAGE_KEY = 'baidupcs.android.importCleanupRecords'
const IMPORT_CLEANUP_STALE_MS = 7 * 24 * 60 * 60 * 1000
const androidImportEnabled = computed(() => canImportFromAndroid())
const chooserDescription = computed(() => androidImportEnabled.value
  ? '选择后会先导入到 App 专属空间，确认上传后才会自动加入队列。'
  : '选择文件或文件夹后，会直接加入上传队列。')
const activeCount = computed(() => uploadItems.value.filter((item) => item.status === 'uploading' || item.status === 'encrypting').length)
const completedCount = computed(() => uploadItems.value.filter((item) => item.status === 'completed').length)
const failedCount = computed(() => uploadItems.value.filter((item) => item.status === 'failed').length)
const pausedCount = computed(() => uploadItems.value.filter((item) => item.status === 'paused').length)
const hasActiveTasks = computed(() => uploadItems.value.some((item) => ['uploading', 'pending', 'encrypting', 'checking_rapid'].includes(item.status)))
const activeCountType = computed(() => activeCount.value === 0 ? 'info' : activeCount.value <= 3 ? 'success' : 'warning')
const uploadPickerInitialPath = computed(() => uploadConfig.value?.recent_directory)
const previewImportedEntries = computed(() => pendingImportedEntries.value.slice(0, 4))

interface ImportedCleanupRecord {
  path: string
  entryType: 'file' | 'directory'
  taskIds: string[]
  createdAt: number
  seenTask: boolean
}

function normalizeRemoteBasePath(path: string) {
  if (!path || path === '/') return '/'
  return path.endsWith('/') ? path.slice(0, -1) : path
}

function getFilename(path: string) {
  return extractFilename(path)
}

function getProgressStatus(status: UploadTaskStatus): 'success' | 'exception' | 'warning' | undefined {
  if (status === 'completed' || status === 'rapid_upload_success') return 'success'
  if (status === 'failed') return 'exception'
  if (status === 'paused' || status === 'encrypting') return 'warning'
  return undefined
}

function getParentDirectory(filePath: string): string | null {
  const normalizedPath = filePath.replace(/\\/g, '/')
  const lastSlashIndex = normalizedPath.lastIndexOf('/')
  if (lastSlashIndex > 0) return normalizedPath.substring(0, lastSlashIndex)
  if (lastSlashIndex === 0) return '/'
  return null
}

function normalizeLocalPath(path: string) {
  return path.replace(/\\/g, '/').replace(/\/+$/, '')
}

function isPathInside(parent: string, child: string) {
  const normalizedParent = normalizeLocalPath(parent)
  const normalizedChild = normalizeLocalPath(child)
  return normalizedChild === normalizedParent || normalizedChild.startsWith(`${normalizedParent}/`)
}

function isActiveUploadStatus(status: UploadTaskStatus) {
  return ['pending', 'checking_rapid', 'encrypting', 'uploading', 'paused'].includes(status)
}

function isSuccessfulUploadStatus(status: UploadTaskStatus) {
  return status === 'completed' || status === 'rapid_upload_success'
}

function readImportedCleanupRecords(): ImportedCleanupRecord[] {
  try {
    const raw = window.localStorage.getItem(IMPORT_CLEANUP_STORAGE_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed.flatMap((record): ImportedCleanupRecord[] => {
      if (!record || typeof record.path !== 'string') return []
      if (record.entryType !== 'file' && record.entryType !== 'directory') return []
      return [{
        path: record.path,
        entryType: record.entryType,
        taskIds: Array.isArray(record.taskIds) ? record.taskIds.filter((id: unknown) => typeof id === 'string') : [],
        createdAt: typeof record.createdAt === 'number' ? record.createdAt : Date.now(),
        seenTask: Boolean(record.seenTask),
      }]
    })
  } catch (error) {
    console.warn('Failed to read Android import cleanup records', error)
    return []
  }
}

function writeImportedCleanupRecords(records: ImportedCleanupRecord[]) {
  try {
    if (records.length === 0) {
      window.localStorage.removeItem(IMPORT_CLEANUP_STORAGE_KEY)
      return
    }
    window.localStorage.setItem(IMPORT_CLEANUP_STORAGE_KEY, JSON.stringify(records))
  } catch (error) {
    console.warn('Failed to persist Android import cleanup records', error)
  }
}

function rememberImportedCleanupRecords(records: ImportedCleanupRecord[]) {
  if (records.length === 0) return

  const existing = readImportedCleanupRecords()
  const merged = new Map<string, ImportedCleanupRecord>()
  for (const record of existing) merged.set(record.path, record)
  for (const record of records) {
    const previous = merged.get(record.path)
    merged.set(record.path, {
      ...record,
      taskIds: Array.from(new Set([...(previous?.taskIds || []), ...record.taskIds])),
      seenTask: previous?.seenTask || record.seenTask,
      createdAt: Math.min(previous?.createdAt || record.createdAt, record.createdAt),
    })
  }
  writeImportedCleanupRecords(Array.from(merged.values()))
}

function cleanupImportedPaths(paths: string[]) {
  const uniquePaths = Array.from(new Set(paths.filter(Boolean)))
  if (uniquePaths.length === 0) return false
  const accepted = cleanupImportedPathsInAndroid(uniquePaths)
  if (!accepted) return false

  const cleanupSet = new Set(uniquePaths)
  writeImportedCleanupRecords(readImportedCleanupRecords().filter((record) => !cleanupSet.has(record.path)))
  return true
}

function cleanupImportedRecordForTask(taskId: string) {
  if (!taskId) return
  const records = readImportedCleanupRecords()
  const matched = records.filter((record) => record.taskIds.includes(taskId))
  if (matched.length === 0) return
  cleanupImportedPaths(matched.map((record) => record.path))
}

function reconcileImportedCleanupRecords(tasks: UploadTask[] = uploadItems.value) {
  const records = readImportedCleanupRecords()
  if (records.length === 0) return

  const now = Date.now()
  const cleanupPaths: string[] = []
  const retained: ImportedCleanupRecord[] = []

  for (const record of records) {
    if (record.taskIds.length > 0) {
      const matchedTasks = tasks.filter((task) => record.taskIds.includes(task.id))
      const hasSuccessfulTask = matchedTasks.some((task) => isSuccessfulUploadStatus(task.status))
      const hasActiveTask = matchedTasks.some((task) => isActiveUploadStatus(task.status))
      const seenTask = record.seenTask || matchedTasks.length > 0

      if (hasSuccessfulTask && !hasActiveTask) {
        cleanupPaths.push(record.path)
      } else if (!seenTask && now - record.createdAt > IMPORT_CLEANUP_STALE_MS) {
        cleanupPaths.push(record.path)
      } else {
        retained.push({ ...record, seenTask })
      }
      continue
    }

    const matchedTasks = tasks.filter((task) => isPathInside(record.path, task.local_path))
    const hasActiveTask = matchedTasks.some((task) => isActiveUploadStatus(task.status))
    const hasFailedTask = matchedTasks.some((task) => task.status === 'failed')
    const hasSuccessfulTask = matchedTasks.some((task) => isSuccessfulUploadStatus(task.status))
    const seenTask = record.seenTask || matchedTasks.length > 0

    if (!hasActiveTask && !hasFailedTask && (hasSuccessfulTask || (seenTask && matchedTasks.length === 0))) {
      cleanupPaths.push(record.path)
    } else if (!seenTask && now - record.createdAt > IMPORT_CLEANUP_STALE_MS) {
      cleanupPaths.push(record.path)
    } else {
      retained.push({ ...record, seenTask })
    }
  }

  writeImportedCleanupRecords(retained)
  if (cleanupPaths.length > 0 && !cleanupImportedPaths(cleanupPaths)) {
    writeImportedCleanupRecords(records)
  }
}

async function loadUploadSettings() {
  try {
    const config = await getConfig()
    uploadConfig.value = config.upload
    uploadConflictStrategy.value = config.conflict_strategy?.default_upload_strategy || 'smart_dedup'
    confirmConflictStrategy.value = uploadConflictStrategy.value
    if (targetRemotePath.value === '/' && config.transfer?.recent_save_path) targetRemotePath.value = config.transfer.recent_save_path
    if (targetRemoteFsId.value === 0 && typeof config.transfer?.recent_save_fs_id === 'number') targetRemoteFsId.value = config.transfer.recent_save_fs_id
  } catch (error) {
    console.error('加载上传配置失败:', error)
  }

  try {
    const encryptionStatus = await getEncryptionStatus()
    hasEncryptionKey.value = encryptionStatus.has_key
  } catch (error) {
    console.error('加载加密状态失败:', error)
    hasEncryptionKey.value = false
  }
}

async function createUploadTasks(
  entries: FileEntry[],
  encrypt = false,
  conflictStrategy: UploadConflictStrategy = uploadConflictStrategy.value,
  cleanupImportedSources = false,
) {
  if (entries.length === 0) return
  uploadConflictStrategy.value = conflictStrategy
  confirmConflictStrategy.value = conflictStrategy

  const basePath = normalizeRemoteBasePath(targetRemotePath.value)
  let successCount = 0
  let failedCountLocal = 0
  const cleanupRecords: ImportedCleanupRecord[] = []

  if (entries.length > 1) ElMessage.info(`正在添加 ${entries.length} 个${encrypt ? '加密' : ''}上传任务...`)

  for (const entry of entries) {
    try {
      const remoteTarget = basePath === '/' ? `/${entry.name}` : `${basePath}/${entry.name}`
      if (entry.entryType === 'file') {
        const taskId = await createUpload({ local_path: entry.path, remote_path: remoteTarget, encrypt, conflict_strategy: conflictStrategy })
        if (cleanupImportedSources) {
          cleanupRecords.push({
            path: entry.path,
            entryType: entry.entryType,
            taskIds: [taskId],
            createdAt: Date.now(),
            seenTask: false,
          })
        }
      } else {
        await createFolderUpload({ local_folder: entry.path, remote_folder: remoteTarget, encrypt, conflict_strategy: conflictStrategy })
        if (cleanupImportedSources) {
          cleanupRecords.push({
            path: entry.path,
            entryType: entry.entryType,
            taskIds: [],
            createdAt: Date.now(),
            seenTask: false,
          })
        }
      }
      successCount++
    } catch (error) {
      failedCountLocal++
      console.error(`上传任务创建失败: ${entry.name}`, error)
    }
  }

  rememberImportedCleanupRecords(cleanupRecords)

  if (successCount === entries.length) {
    ElMessage.success(entries.length === 1 ? '已加入上传队列' : `成功添加 ${successCount} 个上传任务`)
  } else if (successCount > 0) {
    ElMessage.warning(`成功 ${successCount} 个，失败 ${failedCountLocal} 个`)
  } else {
    ElMessage.error('创建上传任务失败')
  }

  if (successCount > 0) {
    const parentDir = getParentDirectory(entries[0].path)
    if (parentDir) {
      updateRecentDirDebounced({ dir_type: 'upload', path: parentDir })
      if (uploadConfig.value) uploadConfig.value.recent_directory = parentDir
    }
    await refreshTasks()
  }
}

function openUploadChooser() {
  uploadChooserVisible.value = true
}

function openDesktopFilePicker(selectType: PickerSelectType) {
  desktopPickerSelectType.value = selectType
  uploadChooserVisible.value = false
  showDesktopFilePicker.value = true
}

function handleChooseFiles() {
  if (androidImportEnabled.value) {
    uploadChooserVisible.value = false
    if (!importFilesFromAndroid()) ElMessage.warning('无法打开安卓文件选择器，请稍后重试')
    return
  }
  openDesktopFilePicker('file')
}

function handleChooseFolders() {
  if (androidImportEnabled.value) {
    uploadChooserVisible.value = false
    if (!importFolderFromAndroid()) ElMessage.warning('无法打开安卓目录选择器，请稍后重试')
    return
  }
  openDesktopFilePicker('directory')
}

async function handleFilePickerSelect(entry: FileEntry, encrypt = false, conflictStrategy?: string) {
  await createUploadTasks([entry], encrypt, (conflictStrategy as UploadConflictStrategy | undefined) || uploadConflictStrategy.value)
}

async function handleFilePickerMultiSelect(entries: FileEntry[], encrypt = false, conflictStrategy?: string) {
  await createUploadTasks(entries, encrypt, (conflictStrategy as UploadConflictStrategy | undefined) || uploadConflictStrategy.value)
}

function normalizeImportedEntries(detail: AndroidImportCompleteDetail | null | undefined): FileEntry[] {
  if (!detail || !Array.isArray(detail.entries)) return []
  const timestamp = new Date().toISOString()
  return detail.entries.flatMap((entry: AndroidImportedEntry) => {
    if (!entry || typeof entry.name !== 'string' || typeof entry.path !== 'string') return []
    if (entry.entryType !== 'file' && entry.entryType !== 'directory') return []
    return [{
      id: entry.path,
      name: entry.name,
      path: entry.path,
      entryType: entry.entryType,
      size: null,
      createdAt: timestamp,
      updatedAt: timestamp,
    } satisfies FileEntry]
  })
}

function handleAndroidImportComplete(event: Event) {
  const entries = normalizeImportedEntries((event as CustomEvent<AndroidImportCompleteDetail>).detail)
  if (entries.length === 0) return
  pendingImportedEntries.value = entries
  confirmEncryptEnabled.value = false
  confirmConflictStrategy.value = uploadConflictStrategy.value
  uploadChooserVisible.value = false
  showDesktopFilePicker.value = false
  confirmUploadVisible.value = true
}

function cancelPendingUpload() {
  confirmUploadVisible.value = false
  pendingImportedEntries.value = []
  confirmSubmitting.value = false
}

async function confirmPendingUpload() {
  if (pendingImportedEntries.value.length === 0 || confirmSubmitting.value) return
  confirmSubmitting.value = true
  try {
    await createUploadTasks(pendingImportedEntries.value, confirmEncryptEnabled.value, confirmConflictStrategy.value, true)
    confirmUploadVisible.value = false
    pendingImportedEntries.value = []
  } finally {
    confirmSubmitting.value = false
  }
}

async function refreshTasks() {
  if (loading.value) return
  loading.value = true
  try {
    uploadItems.value = await getAllUploads()
    reconcileImportedCleanupRecords(uploadItems.value)
  } catch (error) {
    console.error('刷新任务列表失败:', error)
    uploadItems.value = []
  } finally {
    loading.value = false
    updateAutoRefresh()
  }
}

function updateAutoRefresh() {
  if (!isPageVisible.value) {
    if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
    return
  }
  if (wsConnected.value) {
    if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
    return
  }
  if (hasActiveTasks.value) {
    if (!refreshTimer) refreshTimer = window.setInterval(() => refreshTasks(), FALLBACK_REFRESH_MS)
  } else if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

async function handlePause(item: UploadTask) {
  try {
    await pauseUpload(item.id)
    ElMessage.success('任务已暂停')
    refreshTasks()
  } catch (error) {
    console.error('暂停任务失败:', error)
  }
}

async function handleResume(item: UploadTask) {
  try {
    await resumeUpload(item.id)
    ElMessage.success(item.status === 'failed' ? '任务正在重试' : '任务已继续')
    refreshTasks()
  } catch (error) {
    console.error('恢复任务失败:', error)
  }
}

async function handleDelete(item: UploadTask) {
  try {
    await ElMessageBox.confirm('确定要删除此上传任务吗？', '删除确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    await deleteUpload(item.id)
    cleanupImportedRecordForTask(item.id)
    ElMessage.success('任务已删除')
    refreshTasks()
  } catch (error) {
    if (error !== 'cancel') console.error('删除任务失败:', error)
  }
}

async function handleClearCompleted() {
  try {
    await ElMessageBox.confirm(`确定要清除所有已完成的任务吗？（共 ${completedCount.value} 个）`, '批量清除', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const completedTaskIds = uploadItems.value
      .filter((item) => isSuccessfulUploadStatus(item.status))
      .map((item) => item.id)
    const count = await clearCompleted()
    completedTaskIds.forEach(cleanupImportedRecordForTask)
    ElMessage.success(`已清除 ${count} 个任务`)
    refreshTasks()
  } catch (error) {
    if (error !== 'cancel') console.error('清除已完成任务失败:', error)
  }
}

async function handleClearFailed() {
  try {
    await ElMessageBox.confirm(`确定要清除所有失败的任务吗？（共 ${failedCount.value} 个）`, '批量清除', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const failedTaskIds = uploadItems.value
      .filter((item) => item.status === 'failed')
      .map((item) => item.id)
    const count = await clearFailed()
    failedTaskIds.forEach(cleanupImportedRecordForTask)
    ElMessage.success(`已清除 ${count} 个任务`)
    refreshTasks()
  } catch (error) {
    if (error !== 'cancel') console.error('清除失败任务失败:', error)
  }
}

function handleBatchCommand(command: string) {
  if (command === 'pause') handleBatchPause()
  if (command === 'resume') handleBatchResume()
  if (command === 'clearCompleted') handleClearCompleted()
  if (command === 'clearFailed') handleClearFailed()
}

async function handleBatchPause() {
  try {
    const res = await batchPauseUploads({ all: true })
    ElMessage.success(`已暂停 ${res.success_count} 个任务`)
    refreshTasks()
  } catch (error) {
    console.error('批量暂停失败:', error)
  }
}

async function handleBatchResume() {
  try {
    const res = await batchResumeUploads({ all: true })
    ElMessage.success(`已恢复 ${res.success_count} 个任务`)
    refreshTasks()
  } catch (error) {
    console.error('批量恢复失败:', error)
  }
}

function handleUploadEvent(event: UploadEvent) {
  switch (event.event_type) {
    case 'created':
    case 'failed':
      refreshTasks()
      break
    case 'completed':
      cleanupImportedRecordForTask(event.task_id)
      refreshTasks()
      break
    case 'progress': {
      const index = uploadItems.value.findIndex((task) => task.id === event.task_id)
      if (index !== -1) {
        uploadItems.value[index].uploaded_size = event.uploaded_size
        uploadItems.value[index].total_size = event.total_size
        uploadItems.value[index].speed = event.speed
        if (uploadItems.value[index].status === 'encrypting') uploadItems.value[index].status = 'uploading'
      }
      break
    }
    case 'encrypt_progress': {
      const index = uploadItems.value.findIndex((task) => task.id === event.task_id)
      if (index !== -1) {
        uploadItems.value[index].encrypt_progress = event.encrypt_progress
        uploadItems.value[index].status = 'encrypting'
      }
      break
    }
    case 'encrypt_completed': {
      const index = uploadItems.value.findIndex((task) => task.id === event.task_id)
      if (index !== -1) {
        uploadItems.value[index].encrypt_progress = 100
        uploadItems.value[index].original_size = event.original_size
        uploadItems.value[index].status = 'uploading'
      }
      break
    }
    case 'status_changed': {
      const index = uploadItems.value.findIndex((task) => task.id === event.task_id)
      if (index !== -1) uploadItems.value[index].status = event.new_status as UploadTaskStatus
      if (isSuccessfulUploadStatus(event.new_status as UploadTaskStatus)) cleanupImportedRecordForTask(event.task_id)
      break
    }
    case 'paused': {
      const index = uploadItems.value.findIndex((task) => task.id === event.task_id)
      if (index !== -1) {
        uploadItems.value[index].status = 'paused'
        uploadItems.value[index].speed = 0
      }
      break
    }
    case 'resumed': {
      const index = uploadItems.value.findIndex((task) => task.id === event.task_id)
      if (index !== -1) uploadItems.value[index].status = 'uploading'
      break
    }
    case 'deleted':
      cleanupImportedRecordForTask(event.task_id)
      uploadItems.value = uploadItems.value.filter((task) => task.id !== event.task_id)
      break
  }
}

function setupWebSocketSubscriptions() {
  const wsClient = getWebSocketClient()
  wsClient.subscribe(['upload:*'])
  unsubscribeUpload = wsClient.onUploadEvent(handleUploadEvent)
  unsubscribeConnectionState = wsClient.onConnectionStateChange((state: ConnectionState) => {
    const wasConnected = wsConnected.value
    wsConnected.value = state === 'connected'
    updateAutoRefresh()
    if (!wasConnected && wsConnected.value) refreshTasks()
  })
  connectWebSocket()
}

function cleanupWebSocketSubscriptions() {
  const wsClient = getWebSocketClient()
  wsClient.unsubscribe(['upload:*'])
  if (unsubscribeUpload) { unsubscribeUpload(); unsubscribeUpload = null }
  if (unsubscribeConnectionState) { unsubscribeConnectionState(); unsubscribeConnectionState = null }
}

watch(isPageVisible, (visible) => {
  updateAutoRefresh()
  if (visible) refreshTasks()
})

onMounted(async () => {
  await Promise.all([loadUploadSettings(), refreshTasks()])
  if (!hasActiveTasks.value) cleanupStaleImportsInAndroid()
  window.addEventListener(ANDROID_IMPORT_COMPLETE_EVENT, handleAndroidImportComplete as EventListener)
  setupWebSocketSubscriptions()
})

onUnmounted(() => {
  window.removeEventListener(ANDROID_IMPORT_COMPLETE_EVENT, handleAndroidImportComplete as EventListener)
  if (refreshTimer) clearInterval(refreshTimer)
  cleanupWebSocketSubscriptions()
})
</script>

<style scoped lang="scss">
.uploads-container { display:flex; flex-direction:column; gap:16px; width:100%; height:100%; min-height:0; padding:16px; }
.status-card, .task-card { border:1px solid var(--app-border); border-radius:24px; background:var(--app-surface); box-shadow:var(--app-shadow); }
.status-card { display:flex; flex-direction:column; gap:16px; padding:20px; }
.status-head, .status-body { display:flex; justify-content:space-between; gap:16px; }
.status-copy { display:flex; flex-direction:column; gap:6px; }
.status-copy h2, .sheet-card h4 { margin:0; color:var(--app-text); }
.status-copy h2 { font-size:24px; }
.status-description, .field-hint, .sheet-copy, .summary-more { color:var(--app-text-secondary); line-height:1.7; }
.eyebrow { margin:0; font-size:12px; font-weight:700; letter-spacing:.08em; text-transform:uppercase; color:var(--app-accent); }
.target-panel, .summary-card, .inline-option { border:1px solid var(--app-border); border-radius:18px; background:var(--app-surface-strong); }
.target-panel, .confirm-field { display:flex; flex-direction:column; gap:10px; }
.target-panel { flex:1; padding:16px; }
.field-label { font-size:13px; font-weight:700; color:var(--app-text); }
.status-actions, .task-actions, .confirm-actions, .summary-head { display:flex; align-items:center; gap:10px; }
.status-actions, .confirm-actions { justify-content:flex-end; }
.task-container { flex:1; min-height:0; overflow:auto; padding:4px; }
.task-list { display:flex; flex-direction:column; gap:15px; }
.task-card { transition:transform .25s ease, box-shadow .25s ease, border-color .25s ease; }
.task-card.task-active { border-color:rgba(15,118,110,.32); box-shadow:0 12px 32px rgba(15,118,110,.12); }
.task-card:hover { transform:translateY(-2px); }
.task-header { display:flex; justify-content:space-between; gap:16px; margin-bottom:15px; }
.task-info { flex:1; min-width:0; }
.task-title { display:flex; align-items:center; gap:10px; margin-bottom:8px; }
.file-icon { color:var(--app-accent); }
.filename { font-size:16px; font-weight:600; color:var(--app-text); white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.task-path { font-size:12px; color:var(--app-text-secondary); white-space:nowrap; overflow:hidden; text-overflow:ellipsis; padding-left:30px; }
.task-stats { display:flex; flex-wrap:wrap; gap:20px; }
.stat-item { display:flex; align-items:center; gap:6px; font-size:13px; }
.stat-label { color:var(--app-text-secondary); }
.stat-label.error, .stat-value.error { color:#f56c6c; }
.stat-value { color:var(--app-text); font-weight:600; }
.stat-value.speed { color:var(--app-accent); }
.encrypt-progress { margin-bottom:15px; padding:12px; border-radius:14px; background:rgba(245,158,11,.12); }
.encrypt-header { display:flex; align-items:center; gap:8px; margin-bottom:8px; color:#e6a23c; }
.mobile-upload-fab { position:fixed; right:16px; bottom:calc(var(--app-tabbar-height) + env(safe-area-inset-bottom, 0) + 16px); z-index:1200; box-shadow:0 16px 36px rgba(15,118,110,.28); }
.sheet-card { display:flex; flex-direction:column; gap:14px; padding:12px 16px calc(20px + env(safe-area-inset-bottom, 0)); }
.dialog-card { padding:8px 4px 4px; }
.sheet-handle { width:44px; height:5px; border-radius:999px; background:var(--app-border); align-self:center; }
.sheet-actions, .summary-list, .confirm-fields { display:flex; flex-direction:column; gap:10px; }
.confirm-card { gap:16px; }
.summary-card { padding:14px; }
.summary-item { display:flex; align-items:center; gap:8px; color:var(--app-text); font-size:13px; }
.confirm-inline { display:flex; flex-wrap:wrap; gap:12px; }
.inline-option { display:flex; align-items:center; justify-content:space-between; gap:12px; padding:12px 14px; min-height:52px; color:var(--app-text); }
.strategy-option { flex:1; min-width:220px; }
:global(.upload-drawer .el-drawer) { border-top-left-radius:24px; border-top-right-radius:24px; overflow:hidden; background:var(--app-surface); }
:global(.upload-drawer .el-drawer__body) { padding:0 !important; background:var(--app-surface); }
:global(.upload-dialog .el-dialog) { border-radius:24px; overflow:hidden; background:var(--app-surface); }
:global(.upload-dialog .el-dialog__body) { padding:20px; }
@media (max-width: 767px) {
  .uploads-container { gap:12px; padding:12px; }
  .status-card { padding:16px; }
  .status-head, .status-body, .task-header, .confirm-inline { flex-direction:column; }
  .status-actions { justify-content:flex-end; }
  .task-path { padding-left:0; white-space:normal; word-break:break-all; }
  .task-actions { flex-wrap:wrap; }
  .task-stats { gap:12px; }
  .confirm-actions { flex-direction:column-reverse; }
  .strategy-option { min-width:0; }
}
</style>
