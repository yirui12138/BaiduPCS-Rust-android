<!--
SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
SPDX-License-Identifier: Apache-2.0

This file is part of the Android port in this repository.
Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
for Android integration, mobile UX, and distribution compliance.
See the repository LICENSE and NOTICE files for details.
-->

<template>
  <div class="shares-container" :class="{ 'is-mobile': isMobile, 'is-embedded': embedded }">
    <!-- 页面标题和操作栏 -->
    <div class="page-header">
      <div class="header-left">
        <h2 v-if="!isMobile && !embedded">我的分享</h2>
        <el-tag :type="activeCountType" size="large">
          {{ total }} 条分享记录
        </el-tag>
      </div>
      <div class="header-right">
        <!-- PC端按钮 -->
        <template v-if="!isMobile">
          <el-button @click="refreshList" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button
            v-if="selectedIds.length > 0"
            type="danger"
            @click="handleBatchCancel"
          >
            <el-icon><Delete /></el-icon>
            批量取消 ({{ selectedIds.length }})
          </el-button>
        </template>
        <!-- 移动端按钮 -->
        <template v-else>
          <el-button circle @click="refreshList" :loading="loading">
            <el-icon><Refresh /></el-icon>
          </el-button>
          <el-button
            v-if="selectedIds.length > 0"
            circle
            type="danger"
            @click="handleBatchCancel"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </template>
      </div>
    </div>

    <!-- PC端表格视图 -->
    <el-table
      v-if="!isMobile"
      :data="shareList"
      v-loading="loading"
      @selection-change="handleSelectionChange"
      class="share-table"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column label="文件" prop="typicalPath" min-width="200">
        <template #default="{ row }">
          <span class="file-name">{{ getFileName(row.typicalPath) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="链接" prop="shortlink" min-width="180">
        <template #default="{ row }">
          <el-link type="primary" :href="row.shortlink" target="_blank">
            {{ row.shortlink }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="浏览" prop="viewCount" width="80" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="过期时间" width="160">
        <template #default="{ row }">
          {{ formatExpireTime(row.expiredTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleGetDetail(row)" :icon="Key">
            提取码
          </el-button>
          <el-button size="small" @click="handleCopyLink(row)" :icon="Link">
            复制
          </el-button>
          <el-button size="small" type="danger" @click="handleCancel(row)" :icon="Delete">
            取消
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 移动端卡片列表 -->
    <div v-else class="share-card-list">
      <div
        v-for="item in shareList"
        :key="item.shareId"
        class="share-card"
        :class="{ selected: selectedIds.includes(item.shareId) }"
      >
        <div class="card-header" @click="toggleSelect(item.shareId)">
          <el-checkbox
            :model-value="selectedIds.includes(item.shareId)"
            @click.stop
            @change="() => toggleSelect(item.shareId)"
          />
          <span class="file-name">{{ getFileName(item.typicalPath) }}</span>
          <el-tag size="small" :type="getStatusType(item.status)">
            {{ getStatusText(item.status) }}
          </el-tag>
        </div>

        <div class="card-body">
          <div class="info-row">
            <span class="label">链接:</span>
            <span class="value link-text">{{ item.shortlink }}</span>
          </div>
          <div class="info-row">
            <span class="label">浏览:</span>
            <span class="value">{{ item.viewCount }} 次</span>
          </div>
          <div class="info-row">
            <span class="label">过期:</span>
            <span class="value">{{ formatExpireTime(item.expiredTime) }}</span>
          </div>
        </div>

        <div class="card-actions" @click.stop>
          <el-button size="small" :icon="Key" @click="handleGetDetail(item)">提取码</el-button>
          <el-button size="small" :icon="Link" @click="handleCopyLink(item)">复制</el-button>
          <el-button size="small" type="danger" :icon="Delete" @click="handleCancel(item)">取消</el-button>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-if="shareList.length === 0 && !loading" description="暂无分享记录" />
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        :layout="isMobile ? 'prev, pager, next' : 'total, prev, pager, next'"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 分享详情弹窗 - 移动端底部抽屉 -->
    <el-drawer
      v-if="isMobile"
      v-model="detailVisible"
      title="分享详情"
      direction="btt"
      size="auto"
      class="share-detail-drawer"
    >
      <div class="share-detail-mobile">
        <div class="detail-item">
          <span class="label">链接</span>
          <span class="value">{{ currentShareRecord?.shortlink }}</span>
        </div>
        <div class="detail-item">
          <span class="label">提取码</span>
          <span class="value pwd-highlight">{{ currentDetail?.pwd || '无' }}</span>
        </div>
        <div class="action-buttons">
          <el-button type="primary" @click="copyDetailLink" class="block-btn">复制链接</el-button>
          <el-button type="success" @click="copyDetailAll" class="block-btn">复制链接和提取码</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- PC端详情对话框 -->
    <el-dialog v-else v-model="detailVisible" title="分享详情" width="400px">
      <div class="share-detail-pc">
        <div class="detail-row">
          <span class="label">链接:</span>
          <span class="value">{{ currentShareRecord?.shortlink }}</span>
        </div>
        <div class="detail-row">
          <span class="label">提取码:</span>
          <span class="value pwd-highlight">{{ currentDetail?.pwd || '无' }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="copyDetailLink" :icon="Link">复制链接</el-button>
        <el-button type="primary" @click="copyDetailAll" :icon="DocumentCopy">复制链接和提取码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Delete,
  Key,
  Link,
  DocumentCopy,
} from '@element-plus/icons-vue'
import { useIsMobile } from '@/utils/responsive'
import {
  getShareList,
  getShareDetail,
  cancelShare,
  type ShareRecord,
  type ShareDetailData,
} from '@/api/share'

withDefaults(defineProps<{
  embedded?: boolean
}>(), {
  embedded: false,
})

// 响应式检测
const isMobile = useIsMobile()

// 状态
const loading = ref(false)
const shareList = ref<ShareRecord[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20
const selectedIds = ref<number[]>([])

// 详情弹窗状态
const detailVisible = ref(false)
const currentDetail = ref<ShareDetailData | null>(null)
const currentShareRecord = ref<ShareRecord | null>(null)

// 计算属性
const activeCountType = computed(() => {
  if (total.value === 0) return 'info'
  return 'success'
})

// 获取文件名
function getFileName(path: string): string {
  if (!path) return '未知文件'
  const parts = path.replace(/\\/g, '/').split('/')
  return parts[parts.length - 1] || path
}

// 获取状态类型
function getStatusType(status: number): 'success' | 'danger' | 'warning' | 'info' {
  switch (status) {
    case 0:
      return 'success'
    case 1:
      return 'danger'
    default:
      return 'warning'
  }
}

// 获取状态文本
function getStatusText(status: number): string {
  switch (status) {
    case 0:
      return '正常'
    case 1:
      return '已失效'
    case 3:
      return '审核中'
    default:
      return '未知'
  }
}

// 格式化过期时间
function formatExpireTime(timestamp: number): string {
  if (!timestamp || timestamp === 0) {
    return '永久有效'
  }
  
  // 判断时间戳是秒还是毫秒
  // 如果时间戳大于 10000000000（约 2286 年的秒级时间戳），则认为是毫秒
  const isMilliseconds = timestamp > 10000000000
  const date = new Date(isMilliseconds ? timestamp : timestamp * 1000)
  const now = new Date()
  
  if (date < now) {
    return '已过期'
  }
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// 刷新列表
async function refreshList() {
  loading.value = true
  try {
    const data = await getShareList(currentPage.value)
    shareList.value = data.list
    total.value = data.total
  } catch (error: any) {
    console.error('获取分享列表失败:', error)
    ElMessage.error(error.message || '获取分享列表失败')
  } finally {
    loading.value = false
  }
}

// 页码变化
function handlePageChange(page: number) {
  currentPage.value = page
  refreshList()
}

// 表格选择变化
function handleSelectionChange(selection: ShareRecord[]) {
  selectedIds.value = selection.map(item => item.shareId)
}

// 移动端切换选择
function toggleSelect(shareId: number) {
  const index = selectedIds.value.indexOf(shareId)
  if (index === -1) {
    selectedIds.value.push(shareId)
  } else {
    selectedIds.value.splice(index, 1)
  }
}

// 获取分享详情
async function handleGetDetail(record: ShareRecord) {
  try {
    currentShareRecord.value = record
    const detail = await getShareDetail(record.shareId)
    currentDetail.value = detail
    detailVisible.value = true
  } catch (error: any) {
    console.error('获取分享详情失败:', error)
    ElMessage.error(error.message || '获取分享详情失败')
  }
}

// 复制链接
async function handleCopyLink(record: ShareRecord) {
  try {
    await navigator.clipboard.writeText(record.shortlink)
    ElMessage.success('链接已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

// 复制详情链接
async function copyDetailLink() {
  if (!currentShareRecord.value) return
  try {
    await navigator.clipboard.writeText(currentShareRecord.value.shortlink)
    ElMessage.success('链接已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

// 复制详情链接和提取码
async function copyDetailAll() {
  if (!currentShareRecord.value || !currentDetail.value) return
  const pwd = currentDetail.value.pwd || ''
  // 使用列表中的完整链接，提取码从详情接口获取
  const link = currentShareRecord.value.shortlink
  const linkWithPwd = pwd ? `${link}?pwd=${pwd}` : link
  const text = pwd 
    ? `链接: ${linkWithPwd}\n提取码: ${pwd}`
    : `链接: ${linkWithPwd}`
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('链接和提取码已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

// 取消单个分享
async function handleCancel(record: ShareRecord) {
  try {
    await ElMessageBox.confirm(
      `确定要取消分享 "${getFileName(record.typicalPath)}" 吗？`,
      '取消分享',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    await cancelShare([record.shareId])
    ElMessage.success('分享已取消')
    refreshList()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('取消分享失败:', error)
      ElMessage.error(error.message || '取消分享失败')
    }
  }
}

// 批量取消分享
async function handleBatchCancel() {
  if (selectedIds.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(
      `确定要取消选中的 ${selectedIds.value.length} 个分享吗？`,
      '批量取消分享',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    await cancelShare(selectedIds.value)
    ElMessage.success(`已取消 ${selectedIds.value.length} 个分享`)
    selectedIds.value = []
    refreshList()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('批量取消分享失败:', error)
      ElMessage.error(error.message || '批量取消分享失败')
    }
  }
}

// 组件挂载时加载数据
onMounted(() => {
  refreshList()
})
</script>


<style scoped lang="scss">
/* =====================
   容器样式
   ===================== */
.shares-container {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--app-bg);
  color: var(--app-text);

  &.is-embedded {
    padding: 0;
    background: transparent;
    height: auto;
    min-height: 0;

    .share-card-list {
      overflow-y: visible;
    }
  }
  
  &.is-mobile {
    padding: 12px;

    &.is-embedded {
      padding: 0;
    }
  }
}

/* =====================
   页面头部
   ===================== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid var(--app-border);
  border-radius: 16px;
  background: var(--app-surface);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  
  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: var(--app-text);
  }
}

.header-right {
  display: flex;
  gap: 8px;
}

/* =====================
   PC端表格样式
   ===================== */
.share-table {
  flex: 1;
  
  .file-name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

/* =====================
   移动端卡片列表
   ===================== */
.share-card-list {
  flex: 1;
  overflow-y: auto;
}

.share-card {
  background: var(--app-surface);
  border: 1px solid var(--app-border);
  border-radius: 14px;
  padding: 12px;
  margin-bottom: 12px;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.06);
  transition: all 0.2s;
  color: var(--app-text);
  
  &.selected {
    border: 2px solid var(--el-color-primary);
  }
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  
  .file-name {
    flex: 1;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.card-body {
  .info-row {
    display: flex;
    font-size: 13px;
    margin-bottom: 4px;
  }
  
  .label {
    color: var(--el-text-color-secondary);
    width: 50px;
    flex-shrink: 0;
  }
  
  .value {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .link-text {
    color: var(--el-color-primary);
  }
}

.card-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
  
  .el-button {
    flex: 1;
    min-height: 44px; /* 触摸友好的最小高度 */
  }
}

/* =====================
   分页样式
   ===================== */
.pagination-wrapper {
  padding: 16px 0;
  display: flex;
  justify-content: center;
}

/* =====================
   移动端分享详情
   ===================== */
.share-detail-mobile {
  padding: 16px;
}

.share-detail-mobile .detail-item {
  margin-bottom: 16px;
}

.share-detail-mobile .label {
  display: block;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin-bottom: 4px;
}

.share-detail-mobile .value {
  font-size: 16px;
  word-break: break-all;
}

.share-detail-mobile .pwd-highlight {
  color: var(--el-color-primary);
  font-weight: bold;
  font-size: 20px;
}

.share-detail-mobile .action-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 24px;
}

.block-btn {
  width: 100%;
  min-height: 44px;
}

/* =====================
   PC端分享详情
   ===================== */
.share-detail-pc {
  .detail-row {
    margin-bottom: 16px;
    display: flex;
    align-items: flex-start;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
  
  .label {
    color: var(--el-text-color-secondary);
    width: 70px;
    flex-shrink: 0;
  }
  
  .value {
    word-break: break-all;
  }
  
  .pwd-highlight {
    color: var(--el-color-primary);
    font-weight: bold;
    font-size: 18px;
  }
}

/* =====================
   移动端响应式
   ===================== */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }
  
  .header-left {
    justify-content: space-between;
  }
  
  .header-right {
    justify-content: flex-end;
  }
}

/* =====================
   抽屉样式覆盖
   ===================== */
:global(.share-detail-drawer) {
  background: var(--app-surface-strong) !important;
  color: var(--app-text);

  .el-drawer__header {
    margin-bottom: 0;
    padding: 16px;
    border-bottom: 1px solid var(--app-border);
    color: var(--app-text);
    background: var(--app-surface-strong);
  }
  
  .el-drawer__body {
    padding: 0;
    background: var(--app-surface-strong);
    color: var(--app-text);
  }
}
</style>
