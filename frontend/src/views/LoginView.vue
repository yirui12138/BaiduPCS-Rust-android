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
    class="login-container"
    :class="{ 'is-mobile': isMobile }"
    :style="mobileContainerStyle"
  >
    <div class="login-card" :style="mobileLoginCardStyle">
      <!-- Logo 和标题 -->
      <div class="header">
        <div class="logo">
          <el-icon :size="48" color="#409eff">
            <FolderOpened />
          </el-icon>
        </div>
        <h1>{{ APP_DISPLAY_NAME }}</h1>
        <p class="subtitle">Android 本地版</p>
      </div>

      <!-- 登录方式切换 Tab -->
      <div class="login-tabs">
        <button
            class="tab-btn"
            :class="{ active: activeTab === 'qrcode' }"
            @click="switchTab('qrcode')"
        >
          <el-icon :size="16"><Camera /></el-icon>
          <span>扫码登录</span>
        </button>
        <button
            class="tab-btn"
            :class="{ active: activeTab === 'cookie' }"
            @click="switchTab('cookie')"
        >
          <el-icon :size="16"><Key /></el-icon>
          <span>Cookie 登录</span>
        </button>
      </div>

      <!-- 二维码区域 -->
      <div v-show="activeTab === 'qrcode'" class="qrcode-section">
        <div v-if="loading" class="loading">
          <el-icon class="is-loading" :size="32">
            <Loading />
          </el-icon>
          <p>生成二维码中...</p>
          <span class="loading-subtitle">正在准备安全登录会话</span>
        </div>

        <div v-else-if="error" class="error-state">
          <el-icon :size="48" color="#f56c6c">
            <CircleClose />
          </el-icon>
          <p class="error-text">{{ error }}</p>
          <div class="error-actions">
            <el-button type="primary" @click="refreshQRCode">
              <el-icon><Refresh /></el-icon>
              重新生成二维码
            </el-button>
            <el-button @click="switchTab('cookie')">改用网页登录</el-button>
          </div>
        </div>

        <div v-else-if="qrcode" class="qrcode-content">
          <div class="qr-status-card" :class="qrStageClass">
            <div class="status-indicator">
              <el-icon v-if="qrStageClass === 'is-success'" :size="20"><SuccessFilled /></el-icon>
              <el-icon v-else-if="qrStageClass === 'is-expired'" :size="20"><RefreshRight /></el-icon>
              <el-icon v-else-if="qrStageClass === 'is-scanned'" :size="20"><SuccessFilled /></el-icon>
              <el-icon v-else :size="20"><Camera /></el-icon>
            </div>
            <div class="status-copy">
              <strong>{{ qrStageTitle }}</strong>
              <span>{{ qrStageDescription }}</span>
            </div>
          </div>

          <div class="qrcode-image" :style="mobileQrCodeStyle">
            <img :src="qrcodeUrl" alt="登录二维码" />

            <div v-if="isExpired" class="expired-mask">
              <el-icon :size="48" color="#ffffff">
                <RefreshRight />
              </el-icon>
              <p>二维码已过期</p>
              <el-button type="primary" size="large" @click="refreshQRCode">
                刷新二维码
              </el-button>
            </div>

            <div v-else-if="authStore.loginFinalizing" class="scanned-mask finalizing-mask">
              <el-icon :size="48" color="#ffffff">
                <SuccessFilled />
              </el-icon>
              <p class="success-text">授权成功</p>
              <p class="hint-text">{{ authStore.loginFinalizingMessage }}</p>
            </div>

            <div v-else-if="isScanned" class="scanned-mask">
              <el-icon :size="48" color="#ffffff">
                <SuccessFilled />
              </el-icon>
              <p class="success-text">扫描成功</p>
              <p class="hint-text">请在手机百度网盘 App 中确认登录</p>
              <el-button type="success" size="large" plain @click="refreshQRCode">
                <el-icon><Refresh /></el-icon>
                重新扫码
              </el-button>
            </div>
          </div>

          <div class="login-steps" :class="{ 'is-scanned': isScanned, 'is-finalizing': authStore.loginFinalizing }">
            <span class="step-chip is-done">1 打开百度网盘 App</span>
            <span class="step-chip" :class="{ 'is-done': isScanned || authStore.loginFinalizing }">2 扫码</span>
            <span class="step-chip" :class="{ 'is-active': isScanned && !authStore.loginFinalizing, 'is-done': authStore.loginFinalizing }">3 手机确认</span>
            <span class="step-chip" :class="{ 'is-active': authStore.loginFinalizing }">4 自动进入</span>
          </div>

          <div class="countdown">
            <span>{{ countdown }}秒后自动刷新</span>
          </div>
        </div>
      </div>

      <!-- Cookie 登录区域 -->
      <div v-show="activeTab === 'cookie'" class="cookie-section">
        <div v-if="androidCookieLoginAvailable" class="android-cookie-card">
          <div class="android-cookie-header">
            <el-icon :size="20"><Key /></el-icon>
            <div>
              <strong>百度网盘网页登录</strong>
              <p>打开百度网盘官方移动登录页。登录完成后，本应用会读取本机 WebView Cookie 用于本应用登录，不上传到第三方。</p>
            </div>
          </div>
          <div v-if="cookieLoading" class="cookie-login-progress">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>等待网页登录完成，完成后会自动回到应用</span>
          </div>
          <el-button
              type="primary"
              size="large"
              :loading="cookieLoading"
              class="cookie-login-btn"
              @click="startAndroidCookieLogin"
          >
            打开百度网盘登录页
          </el-button>
        </div>
        <div v-if="cookieError && androidCookieLoginAvailable" class="cookie-error">
          <el-icon :size="14" color="#f56c6c"><CircleClose /></el-icon>
          <span>{{ cookieError }}</span>
        </div>

        <el-collapse
          v-if="androidCookieLoginAvailable"
          v-model="manualCookiePanels"
          class="manual-cookie-collapse"
        >
          <el-collapse-item title="高级备用方式：手动粘贴 Cookie" name="manual">
            <div class="manual-cookie-body">
              <div class="cookie-tips compact">
                <div class="cookie-tips-title">
                  <el-icon :size="16" color="#409eff"><InfoFilled /></el-icon>
                  <span>网页登录不可用时再使用此方式</span>
                </div>
                <p>请从已登录的浏览器请求头中复制完整 <code>cookie</code> 值，粘贴到下方。</p>
              </div>
              <div class="cookie-input-wrap">
                <el-input
                    v-model="cookieInput"
                    type="textarea"
                    :rows="5"
                    placeholder="粘贴完整 Cookie 字符串，例如：&#10;BDUSS=xxxx; PTOKEN=yyyy; STOKEN=zzzz; BAIDUID=aaaa"
                    resize="none"
                    :disabled="cookieLoading"
                />
              </div>
              <el-button
                  type="primary"
                  size="large"
                  :loading="cookieLoading"
                  :disabled="!cookieInput.trim()"
                  class="cookie-login-btn"
                  @click="loginWithCookie"
              >
                <el-icon v-if="!cookieLoading"><Key /></el-icon>
                {{ cookieLoading ? '登录中...' : '使用 Cookie 登录' }}
              </el-button>
            </div>
          </el-collapse-item>
        </el-collapse>

        <div v-else class="manual-cookie-body is-visible">
          <div class="cookie-tips">
            <div class="cookie-tips-title">
              <el-icon :size="16" color="#409eff"><InfoFilled /></el-icon>
              <span>如何一键获取完整 Cookie？</span>
            </div>
            <ol class="cookie-steps">
              <li>浏览器打开 <strong>pan.baidu.com</strong> 并登录账号</li>
              <li>按 <strong>F12</strong> → 切换到 <strong>Network（网络）</strong> 标签页</li>
              <li>刷新页面，点击列表中任意一个请求（如 <code>netdisk</code> 或 <code>api?...</code>）</li>
              <li>右侧 <strong>Headers → Request Headers</strong> 中找到 <code>cookie</code> 字段</li>
              <li>点击该行右侧的复制按钮，或右键 → 复制值，粘贴到下方即可</li>
            </ol>
            <div class="cookie-tip-note">
              <el-icon :size="13" color="#e6a23c"><Warning /></el-icon>
              <span>整个 <code>cookie</code> 请求头的值就是所需格式，无需手动整理</span>
            </div>
          </div>
          <div class="cookie-input-wrap">
            <el-input
                v-model="cookieInput"
                type="textarea"
                :rows="5"
                placeholder="粘贴完整 Cookie 字符串，例如：&#10;BDUSS=xxxx; PTOKEN=yyyy; STOKEN=zzzz; BAIDUID=aaaa"
                resize="none"
                :disabled="cookieLoading"
            />
          </div>
          <div v-if="cookieError" class="cookie-error">
            <el-icon :size="14" color="#f56c6c"><CircleClose /></el-icon>
            <span>{{ cookieError }}</span>
          </div>
          <el-button
              type="primary"
              size="large"
              :loading="cookieLoading"
              :disabled="!cookieInput.trim()"
              class="cookie-login-btn"
              @click="loginWithCookie"
          >
            <el-icon v-if="!cookieLoading"><Key /></el-icon>
            {{ cookieLoading ? '登录中...' : '使用 Cookie 登录' }}
          </el-button>
        </div>
      </div>

      <!-- 底部信息 -->
      <div class="footer" :style="mobileFooterStyle">
        <p>基于 Rust + Axum + Vue 3 构建</p>
        <p class="version">v{{ appVersion }} · 基于开源项目 {{ UPSTREAM_PROJECT_NAME }} {{ UPSTREAM_VERSION }} 移植</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import type { CSSProperties } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useIsMobile } from '@/utils/responsive'
import {
  ANDROID_COOKIE_LOGIN_RESULT_EVENT,
  canStartBaiduCookieLoginInAndroid,
  startBaiduCookieLoginInAndroid,
  type AndroidCookieLoginResultDetail,
} from '@/utils/androidBridge'
import {
  APP_DISPLAY_NAME,
  UPSTREAM_PROJECT_NAME,
  UPSTREAM_VERSION,
  getAppVersion,
} from '@/constants/appInfo'
import {
  FolderOpened,
  Loading,
  CircleClose,
  Refresh,
  RefreshRight,
  Camera,
  SuccessFilled,
  Warning,
  InfoFilled,
  Key,
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const appVersion = getAppVersion()
const ANDROID_APP_FOREGROUND_EVENT = 'android-app-foreground'

// 响应式检测
const isMobile = useIsMobile()

// 登录方式
const activeTab = ref<'qrcode' | 'cookie'>('qrcode')

// 状态
const loading = ref(false)
const error = ref('')
const qrcode = computed(() => authStore.qrcode)
const isExpired = ref(false)
const isScanned = computed(() => authStore.isQrScanned && !isExpired.value)
const countdown = ref(120)

const mobileContainerStyle = computed<CSSProperties | undefined>(() => {
  if (!isMobile.value) return undefined
  return {
    flexDirection: 'column',
    justifyContent: 'flex-start',
    alignItems: 'stretch',
    gap: '16px',
    padding: 'max(16px, env(safe-area-inset-top, 0px)) 16px calc(24px + env(safe-area-inset-bottom, 0px))',
  }
})

const mobileLoginCardStyle = computed<CSSProperties | undefined>(() => {
  if (!isMobile.value) return undefined
  return {
    order: 1,
    width: '100%',
    maxWidth: '100%',
    margin: '0',
    padding: '20px 16px',
    borderRadius: '24px',
    boxShadow: '0 18px 44px rgba(15, 23, 42, 0.18)',
  }
})

const mobileQrCodeStyle = computed<CSSProperties | undefined>(() => {
  if (!isMobile.value) return undefined
  return {
    width: 'clamp(180px, 64vw, 220px)',
    height: 'clamp(180px, 64vw, 220px)',
    padding: '12px',
    marginBottom: '0',
  }
})

const mobileFooterStyle = computed<CSSProperties | undefined>(() => {
  if (!isMobile.value) return undefined
  return {
    display: 'none',
  }
})

// Cookie 登录状态
const cookieInput = ref('')
const cookieLoading = ref(false)
const cookieError = ref('')
const manualCookiePanels = ref<string[]>([])
const androidCookieLoginAvailable = computed(() => canStartBaiduCookieLoginInAndroid())
let hasShownQrWaitTip = false
let hasShownScannedWaitTip = false

// 计算二维码URL
const qrcodeUrl = computed(() => {
  if (!qrcode.value) return ''
  return qrcode.value.image_base64
})

const qrStageClass = computed(() => {
  if (isExpired.value) return 'is-expired'
  if (authStore.loginFinalizing) return 'is-success'
  if (isScanned.value) return 'is-scanned'
  if (authStore.qrLoginStatus === 'waiting') return 'is-waiting'
  return 'is-idle'
})

const qrStageTitle = computed(() => {
  if (isExpired.value) return '二维码已过期'
  if (authStore.loginFinalizing) {
    return authStore.loginFinalizingMessage.includes('登录完成') ? '登录完成，正在进入文件页' : '授权成功，正在同步登录'
  }
  if (isScanned.value) return '扫描成功，等待手机确认'
  if (authStore.qrLoginStatus === 'waiting') return '等待扫码'
  return '生成二维码中'
})

const qrStageDescription = computed(() => {
  if (isExpired.value) return '可以重新生成二维码，或改用网页登录。'
  if (authStore.loginFinalizing) return authStore.loginFinalizingMessage || '正在同步本地会话，不需要退出重进。'
  if (isScanned.value) return '请在手机百度网盘 App 确认，确认后本页可能需要几秒同步，请等待片刻。'
  return '打开百度网盘 App 扫一扫。扫码后请回到本页等待片刻，不需要退出重进。'
})

// 倒计时定时器
let countdownTimer: number | null = null
let foregroundPollTimers: number[] = []

function navigateToFiles() {
  authStore.stopPolling()
  stopCountdown()
  authStore.setLoginFinalizing(false)
  router.replace('/files')
}

// 开始倒计时
function startCountdown() {
  countdown.value = 120
  isExpired.value = false

  if (countdownTimer) {
    clearInterval(countdownTimer)
  }

  countdownTimer = window.setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      stopCountdown()
      isExpired.value = true
      authStore.stopPolling()
    }
  }, 1000)
}

// 停止倒计时
function stopCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

function clearForegroundPollTimers() {
  foregroundPollTimers.forEach((timer) => window.clearTimeout(timer))
  foregroundPollTimers = []
}

function isDocumentActive() {
  return typeof document === 'undefined' || document.visibilityState !== 'hidden'
}

function shouldPollQRCodeImmediately() {
  return activeTab.value === 'qrcode' &&
    Boolean(qrcode.value) &&
    authStore.isPolling &&
    !isExpired.value &&
    !authStore.loginFinalizing
}

function pollQRCodeImmediately() {
  if (!shouldPollQRCodeImmediately()) return
  void authStore.pollQRCodeStatusNow()
}

function scheduleForegroundQrPollBurst() {
  if (!isDocumentActive()) return
  clearForegroundPollTimers()
  pollQRCodeImmediately()

  // WebView 刚回前台时网络和 JS 计时器恢复有时不同步，短促补查能消掉 5-10 秒的迟钝感。
  foregroundPollTimers = [250, 900, 1800].map((delayMs) =>
    window.setTimeout(() => pollQRCodeImmediately(), delayMs),
  )
}

function handleForegroundLikeEvent() {
  scheduleForegroundQrPollBurst()
}

// 生成二维码
async function generateQRCode() {
  loading.value = true
  error.value = ''
  isExpired.value = false
  hasShownScannedWaitTip = false

  try {
    await authStore.generateQRCode()
    if (!hasShownQrWaitTip) {
      hasShownQrWaitTip = true
      ElMessage({
        type: 'info',
        message: '扫码后请回到本页等待片刻，状态同步可能需要几秒，不需要退出重进。',
        duration: 6000,
        showClose: true,
      })
    }

    // 开始轮询
    authStore.startPolling(
        // 成功回调
        () => {
          ElMessage.success('登录成功，正在进入文件页')
          window.setTimeout(() => navigateToFiles(), 520)
        },
        // 错误回调
        (err: any) => {
          error.value = err.message || '登录失败，请重试'
          stopCountdown()
        },
        // 扫码回调
        () => {
          navigator.vibrate?.(35)
          if (!hasShownScannedWaitTip) {
            hasShownScannedWaitTip = true
            ElMessage({
              type: 'success',
              message: '扫码已收到。请在手机百度网盘 App 确认登录，然后等待片刻，应用会自动进入文件页。',
              duration: 6500,
              showClose: true,
            })
          }
          scheduleForegroundQrPollBurst()
        },
        { pollIntervalMs: 900 },
    )

    // 开始倒计时
    startCountdown()
  } catch (err: any) {
    error.value = err.message || '生成二维码失败，请重试'
  } finally {
    loading.value = false
  }
}

// 刷新二维码
async function refreshQRCode() {
  authStore.stopPolling()
  stopCountdown()
  await generateQRCode()
}

// 切换登录方式
function switchTab(tab: 'qrcode' | 'cookie') {
  if (activeTab.value === tab) return
  activeTab.value = tab
  cookieError.value = ''
  if (tab === 'cookie') {
    // 切到 Cookie 登录时停止二维码轮询
    authStore.stopPolling()
    stopCountdown()
  } else {
    // 切回二维码时重新生成（如果还没有二维码）
    generateQRCode()
  }
}

async function submitCookieLogin(cookies: string) {
  cookieError.value = ''
  if (!cookies.trim()) return

  cookieLoading.value = true
  try {
    const result = await authStore.loginWithCookies(cookies.trim())
    if (result.message && !result.message.includes('预热完成')) {
      ElMessage({
        type: 'warning',
        message: result.message,
        duration: 8000,
        showClose: true,
      })
    } else {
      ElMessage.success('登录成功，正在进入文件页')
    }
    window.setTimeout(() => navigateToFiles(), 420)
  } catch (err: any) {
    cookieError.value = err.message || 'Cookie 登录失败，请检查 Cookie 是否完整有效'
  } finally {
    cookieLoading.value = false
  }
}

// Cookie 登录
async function loginWithCookie() {
  await submitCookieLogin(cookieInput.value)
}

function startAndroidCookieLogin() {
  cookieError.value = ''
  cookieLoading.value = true
  const accepted = startBaiduCookieLoginInAndroid()
  if (!accepted) {
    cookieLoading.value = false
    cookieError.value = '无法打开百度网盘登录页，请使用下方手动 Cookie 登录'
  }
}

async function handleAndroidCookieLoginResult(event: Event) {
  const detail = (event as CustomEvent<AndroidCookieLoginResultDetail>).detail
  if (!detail) return

  if (detail.status === 'cancelled') {
    cookieLoading.value = false
    ElMessage.info('已取消网页登录')
    return
  }

  if (detail.status !== 'success' || !detail.cookies?.trim()) {
    cookieLoading.value = false
    cookieError.value = detail.reason || '未能从百度网盘网页登录中获取有效 Cookie'
    return
  }

  let importedCookies = detail.cookies
  try {
    await submitCookieLogin(importedCookies)
  } finally {
    importedCookies = ''
  }
}

// 组件挂载
onMounted(async () => {
  window.addEventListener(ANDROID_COOKIE_LOGIN_RESULT_EVENT, handleAndroidCookieLoginResult as EventListener)
  window.addEventListener('focus', handleForegroundLikeEvent)
  window.addEventListener('pageshow', handleForegroundLikeEvent)
  window.addEventListener(ANDROID_APP_FOREGROUND_EVENT, handleForegroundLikeEvent)
  document.addEventListener('visibilitychange', handleForegroundLikeEvent)

  // 检查是否已登录
  if (authStore.isLoggedIn || await authStore.ensureSession()) {
    navigateToFiles()
    return
  }

  // 生成二维码
  await generateQRCode()
})

// 组件卸载
onUnmounted(() => {
  window.removeEventListener(ANDROID_COOKIE_LOGIN_RESULT_EVENT, handleAndroidCookieLoginResult as EventListener)
  window.removeEventListener('focus', handleForegroundLikeEvent)
  window.removeEventListener('pageshow', handleForegroundLikeEvent)
  window.removeEventListener(ANDROID_APP_FOREGROUND_EVENT, handleForegroundLikeEvent)
  document.removeEventListener('visibilitychange', handleForegroundLikeEvent)
  clearForegroundPollTimers()
  authStore.stopPolling()
  stopCountdown()
})
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  min-height: 100dvh;
  background:
    radial-gradient(circle at top, rgba(61, 211, 195, 0.18), transparent 34%),
    linear-gradient(135deg, #0e1f2b 0%, #11485a 52%, #f18847 100%);
  position: relative;
  padding: 24px;
}

.tips-card {
  position: fixed;
  top: 20px;
  right: 20px;
  width: 320px;
  background: var(--app-surface-overlay);
  border-radius: 12px;
  box-shadow: var(--app-shadow);
  overflow: hidden;
  transition: all 0.3s ease;
  z-index: 100;

  &.collapsed {
    width: auto;
  }

  .tips-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    background: linear-gradient(135deg, #0f766e 0%, #1d9a88 100%);
    color: white;
    cursor: pointer;
    user-select: none;

    .tips-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      font-weight: 600;
    }

    .collapse-icon {
      transition: transform 0.3s ease;

      &.rotated {
        transform: rotate(-90deg);
      }
    }

    &:hover {
      background: linear-gradient(135deg, #0b5e58 0%, #147569 100%);
    }
  }

  .tips-content {
    padding: 16px;
    max-height: calc(100vh - 120px);
    overflow-y: auto;

    .tips-section {
      margin-bottom: 16px;

      &:last-child {
        margin-bottom: 0;
      }

      h4 {
        margin: 0 0 8px 0;
        font-size: 14px;
        font-weight: 600;
        color: #333;
      }

      ol, ul {
        margin: 0;
        padding-left: 20px;
        font-size: 13px;
        line-height: 1.8;
        color: #666;

        li {
          margin-bottom: 4px;

          strong {
            color: #333;
            font-weight: 600;
          }
        }
      }

      &.warning {
        background: #fff7e6;
        padding: 12px;
        border-radius: 8px;
        border-left: 3px solid #fa8c16;

        h4 {
          color: #d46b08;
        }

        ul li {
          color: #ad6800;

          strong {
            color: #ad4e00;
          }
        }
      }

      &.info {
        .small-text {
          display: flex;
          align-items: flex-start;
          gap: 6px;
          margin: 0;
          font-size: 12px;
          color: #666;
          line-height: 1.6;
          padding: 10px;
          background: #f0f9ff;
          border-radius: 6px;
        }
      }
    }

    /* 自定义滚动条 */
    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-track {
      background: #f1f1f1;
      border-radius: 3px;
    }

    &::-webkit-scrollbar-thumb {
      background: #c1c1c1;
      border-radius: 3px;

      &:hover {
        background: #a8a8a8;
      }
    }
  }
}

.login-container.is-mobile {
  justify-content: flex-start;
  align-items: stretch;
  gap: 16px;
  padding:
    max(16px, env(safe-area-inset-top, 0px))
    16px
    calc(24px + env(safe-area-inset-bottom, 0px));

  &.tips-expanded {
    padding-bottom: calc(24px + env(safe-area-inset-bottom, 0px));
  }

  .tips-card {
    position: static;
    order: 2;
    width: 100%;
    border-radius: 20px;
    font-size: 12px;
    max-height: none;
    z-index: auto;
    box-shadow: 0 14px 36px rgba(15, 23, 42, 0.14);

    &.collapsed {
      width: 100%;
    }

    .tips-header {
      padding: 14px 16px;

      &::after {
        content: '展开提示';
        font-size: 11px;
        color: rgba(255, 255, 255, 0.8);
        margin-left: auto;
      }
    }

    &:not(.collapsed) .tips-header::after {
      content: '收起提示';
    }

    .tips-content {
      max-height: none;
      overflow: visible;
    }
  }

  .login-card {
    order: 1;
    padding: 20px 16px;
    border-radius: 24px;
    margin: 0;
    width: 100%;
    max-width: 100%;
    box-shadow: 0 18px 44px rgba(15, 23, 42, 0.18);
  }

  .header {
    margin-bottom: 18px;

    .logo {
      margin-bottom: 10px;
    }

    h1 {
      font-size: 22px;
      line-height: 1.3;
    }

    .subtitle {
      font-size: 11px;
    }
  }

  .login-tabs {
    margin-bottom: 16px;
  }

  .tab-btn {
    min-height: 44px;
    font-size: 13px;
  }

  .qrcode-section {
    min-height: auto;
  }

  .qrcode-content {
    gap: 12px;
  }

  .qrcode-image {
    width: clamp(180px, 64vw, 220px);
    height: clamp(180px, 64vw, 220px);
    padding: 12px;
    margin-bottom: 0;

    .expired-mask,
    .scanned-mask {
      p {
        font-size: 14px;
        margin: 12px 0 16px 0;
      }

      .success-text {
        font-size: 16px;
      }

      .hint-text {
        font-size: 13px;
      }
    }
  }

  .loading {
    p {
      font-size: 13px;
    }
  }

  .error-state {
    .error-text {
      font-size: 13px;
      padding: 0 16px;
    }
  }

  .scan-tips {
    margin-bottom: 0;

    .tip-item {
      padding: 10px;
      font-size: 12px;

      &.success {
        padding: 12px;

        .success-header {
          .success-title {
            font-size: 14px;
          }
        }

        .important-notes {
          .note-item {
            padding: 8px 10px;
            font-size: 12px;
            gap: 6px;
          }
        }
      }
    }
  }

  .countdown {
    font-size: 12px;
    margin-top: 2px;
  }

  .footer {
    display: none;
  }
}

/* 小屏幕（手机横屏）适配 */
@media (max-width: 768px) and (max-height: 600px) {
  .login-container {
    padding:
      max(12px, env(safe-area-inset-top, 0px))
      16px
      calc(16px + env(safe-area-inset-bottom, 0px));
  }

  .login-card {
    padding: 20px 16px;
  }

  .header {
    margin-bottom: 16px;
  }

  .qrcode-section {
    min-height: 200px;
  }

  .qrcode-image {
    width: 160px;
    height: 160px;
    padding: 8px;
  }

  .footer {
    margin-top: 16px;
  }
}

/* 超小屏幕（手机竖屏小尺寸）适配 */
@media (max-width: 375px) {
  .login-card {
    padding: 18px 14px;
  }

  .header {
    h1 {
      font-size: 18px;
    }
  }

  .qrcode-image {
    width: clamp(160px, 62vw, 190px);
    height: clamp(160px, 62vw, 190px);
  }

  .scan-tips {
    .tip-item {
      font-size: 11px;

      &.success {
        .important-notes {
          .note-item {
            font-size: 11px;
          }
        }
      }
    }
  }
}

.login-card {
  width: 100%;
  max-width: 480px;
  padding: 40px;
  background: var(--app-surface-overlay);
  border-radius: 20px;
  border: 1px solid var(--app-border);
  box-shadow: var(--app-shadow);
}

.header {
  text-align: center;
  margin-bottom: 40px;

  .logo {
    margin-bottom: 20px;
  }

  h1 {
    margin: 0 0 8px 0;
    font-size: 28px;
    font-weight: 600;
    color: var(--app-text);
  }

  .subtitle {
    margin: 0;
    font-size: 14px;
    color: var(--app-text-secondary);
  }
}

.qrcode-section {
  min-height: 400px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.loading {
  text-align: center;

  p {
    margin-top: 16px;
    margin-bottom: 4px;
    color: var(--app-text);
    font-weight: 700;
  }

  .loading-subtitle {
    color: var(--app-text-secondary);
    font-size: 13px;
  }
}

.error-state {
  text-align: center;

  .error-text {
    margin: 20px 0;
    color: #f56c6c;
    font-size: 14px;
  }

  .error-actions {
    display: flex;
    justify-content: center;
    gap: 10px;
    flex-wrap: wrap;
  }
}

.qrcode-content {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.qr-status-card {
  width: min(100%, 360px);
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid rgba(64, 158, 255, 0.18);
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.1), rgba(61, 211, 195, 0.06));
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  transition: border-color 0.22s ease, background 0.22s ease, transform 0.22s ease;

  &.is-scanned,
  &.is-success {
    border-color: rgba(38, 166, 115, 0.36);
    background: linear-gradient(135deg, rgba(38, 166, 115, 0.14), rgba(61, 211, 195, 0.08));
    transform: translateY(-1px);
  }

  &.is-expired {
    border-color: rgba(245, 108, 108, 0.3);
    background: linear-gradient(135deg, rgba(245, 108, 108, 0.13), rgba(230, 162, 60, 0.08));
  }
}

.status-indicator {
  width: 38px;
  height: 38px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  color: #1677ff;
  background: rgba(64, 158, 255, 0.14);
}

.qr-status-card.is-scanned .status-indicator,
.qr-status-card.is-success .status-indicator {
  color: #18a058;
  background: rgba(24, 160, 88, 0.16);
  animation: scannedPulse 1.8s ease-in-out infinite;
}

.qr-status-card.is-expired .status-indicator {
  color: #f56c6c;
  background: rgba(245, 108, 108, 0.14);
}

.status-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
  text-align: left;

  strong {
    color: var(--app-text);
    font-size: 15px;
    line-height: 1.25;
  }

  span {
    color: var(--app-text-secondary);
    font-size: 12px;
    line-height: 1.45;
  }
}

.qrcode-image {
  position: relative;
  width: 280px;
  height: 280px;
  padding: 20px;
  background: white;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  margin-bottom: 0;

  img {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: contain;
  }

  .expired-mask {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.8);
    border-radius: 12px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    color: white;

    p {
      margin: 16px 0 24px 0;
      font-size: 16px;
    }
  }

  .scanned-mask {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(103, 194, 58, 0.95);
    border-radius: 12px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    color: white;
    animation: fadeIn 0.3s ease-in-out;

    .success-text {
      margin: 16px 0 8px 0;
      font-size: 18px;
      font-weight: 600;
    }

    .hint-text {
      margin: 0 0 24px 0;
      font-size: 14px;
      opacity: 0.9;
    }

    &.finalizing-mask {
      background: linear-gradient(135deg, rgba(24, 160, 88, 0.96), rgba(22, 119, 255, 0.92));
    }
  }
}

.login-steps {
  width: min(100%, 360px);
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.step-chip {
  min-height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px 8px;
  border-radius: 999px;
  background: var(--app-surface-muted);
  border: 1px solid var(--app-border);
  color: var(--app-text-secondary);
  font-size: 12px;
  font-weight: 700;
  transition: color 0.2s ease, background 0.2s ease, border-color 0.2s ease, transform 0.2s ease;

  &.is-done {
    color: #1677ff;
    border-color: rgba(64, 158, 255, 0.28);
    background: rgba(64, 158, 255, 0.1);
  }

  &.is-active {
    color: #18a058;
    border-color: rgba(24, 160, 88, 0.34);
    background: rgba(24, 160, 88, 0.12);
    transform: translateY(-1px);
  }
}

@keyframes scannedPulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(24, 160, 88, 0.28);
  }
  50% {
    box-shadow: 0 0 0 8px rgba(24, 160, 88, 0);
  }
}

.scan-tips {
  width: 100%;
  margin-bottom: 16px;

  .tip-item {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 12px;
    background: #f0f9ff;
    border-radius: 8px;
    font-size: 14px;
    color: #409eff;

    &.success {
      flex-direction: column;
      gap: 12px;
      background: linear-gradient(135deg, #f0f9f0 0%, #e8f5e9 100%);
      border: 2px solid #67c23a;
      padding: 16px;
      animation: pulse 2s ease-in-out infinite;

      .success-header {
        display: flex;
        align-items: center;
        gap: 8px;

        .success-title {
          font-size: 16px;
          font-weight: 600;
          color: #67c23a;
        }
      }

      .important-notes {
        width: 100%;
        display: flex;
        flex-direction: column;
        gap: 10px;
        margin-top: 4px;

        .note-item {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 10px 12px;
          background: #fff;
          border-radius: 6px;
          border-left: 3px solid #e6a23c;
          font-size: 13px;
          color: #333;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

          &.highlight {
            font-weight: 500;
          }

          span {
            flex: 1;
            line-height: 1.5;
          }
        }
      }
    }
  }
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(103, 194, 58, 0.4);
  }
  50% {
    box-shadow: 0 0 0 8px rgba(103, 194, 58, 0);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.countdown {
  text-align: center;
  font-size: 13px;
  color: #999;
}

.footer {
  margin-top: 40px;
  text-align: center;
  font-size: 12px;
  color: var(--app-text-secondary);

  p {
    margin: 4px 0;
  }

  .version {
    font-weight: 600;
    color: var(--app-text);
  }
}

/* ===== 登录方式 Tab ===== */
.login-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 24px;
  border-radius: 10px;
  overflow: hidden;
  border: 1.5px solid var(--app-border);
  background: var(--app-surface-muted);
}

.tab-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 0;
  border: none;
  background: transparent;
  font-size: 14px;
  font-weight: 500;
  color: var(--app-text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover:not(.active) {
    color: var(--app-accent);
    background: var(--app-accent-soft);
  }

  &.active {
    background: var(--app-accent);
    color: white;
  }
}

/* ===== Cookie 登录区域 ===== */
.cookie-section {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 8px;
}

.android-cookie-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  border: 1px solid rgba(64, 158, 255, 0.22);
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.12), rgba(61, 211, 195, 0.08));
}

.android-cookie-header {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  color: #1f2d3d;

  .el-icon {
    margin-top: 2px;
    color: #1677ff;
    flex-shrink: 0;
  }

  strong {
    display: block;
    margin-bottom: 4px;
    font-size: 15px;
  }

  p {
    margin: 0;
    color: #566575;
    font-size: 12px;
    line-height: 1.55;
  }
}

.cookie-login-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 11px;
  border-radius: 10px;
  background: rgba(64, 158, 255, 0.1);
  color: #1677ff;
  font-size: 13px;
  font-weight: 700;
}

.manual-cookie-collapse {
  border: 0;

  :deep(.el-collapse-item__header) {
    height: 44px;
    padding: 0 12px;
    border: 1px solid var(--app-border);
    border-radius: 12px;
    background: var(--app-surface-muted);
    color: var(--app-text);
    font-weight: 700;
  }

  :deep(.el-collapse-item__wrap) {
    border-bottom: 0;
    background: transparent;
  }

  :deep(.el-collapse-item__content) {
    padding: 12px 0 0;
  }
}

.manual-cookie-body {
  display: flex;
  flex-direction: column;
  gap: 12px;

  &.is-visible {
    display: flex;
  }
}

.cookie-tips {
  background: #f0f9ff;
  border: 1px solid #bae0ff;
  border-radius: 8px;
  padding: 14px 16px;

  &.compact {
    padding: 12px 14px;

    p {
      margin: 0;
      color: #566575;
      font-size: 12px;
      line-height: 1.55;
    }
  }

  .cookie-tips-title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    font-weight: 600;
    color: #1677ff;
    margin-bottom: 10px;
  }

  .cookie-steps {
    margin: 0;
    padding-left: 20px;
    font-size: 13px;
    line-height: 1.9;
    color: #555;

    li {
      strong {
        color: #333;
      }
      code {
        background: #e6f4ff;
        border-radius: 3px;
        padding: 1px 5px;
        font-size: 12px;
        color: #0958d9;
        font-family: monospace;
      }
    }
  }

  .cookie-tip-note {
    display: flex;
    align-items: flex-start;
    gap: 6px;
    margin-top: 10px;
    padding: 8px 10px;
    background: #fffbe6;
    border: 1px solid #ffe58f;
    border-radius: 6px;
    font-size: 12px;
    color: #7c4c00;
    line-height: 1.5;

    span {
      flex: 1;
      code {
        background: #fff3cd;
        padding: 0 4px;
        border-radius: 3px;
        font-family: monospace;
      }
    }
  }
}

.cookie-input-wrap {
  :deep(.el-textarea__inner) {
    font-family: monospace;
    font-size: 12px;
    line-height: 1.6;
    border-radius: 8px;
  }
}

.cookie-error {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 10px 12px;
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 6px;
  font-size: 13px;
  color: #cf1322;
  line-height: 1.5;

  span {
    flex: 1;
  }
}

.cookie-login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  border-radius: 8px;
}

html.dark {
  .login-container {
    background:
      radial-gradient(circle at top, rgba(61, 211, 195, 0.14), transparent 30%),
      linear-gradient(180deg, #061118 0%, #0b1e28 100%);
  }

  .login-card {
    box-shadow: none;
  }

  .qr-status-card {
    box-shadow: none;
  }

  .qrcode-image {
    border-color: rgba(148, 163, 184, 0.28);
  }

  .android-cookie-card {
    background: linear-gradient(135deg, rgba(64, 158, 255, 0.16), rgba(61, 211, 195, 0.08));
    border-color: rgba(125, 211, 252, 0.22);
  }

  .android-cookie-header {
    color: rgba(245, 250, 255, 0.94);

    p {
      color: rgba(220, 235, 245, 0.72);
    }
  }

  .cookie-tips {
    background: rgba(17, 39, 57, 0.88);
    border-color: rgba(61, 211, 195, 0.18);

    &.compact p,
    .cookie-steps {
      color: rgba(220, 235, 245, 0.72);
    }
  }

  .cookie-login-progress {
    background: rgba(64, 158, 255, 0.14);
    color: #93c5fd;
  }

  .manual-cookie-collapse {
    :deep(.el-collapse-item__header) {
      background: rgba(15, 23, 42, 0.72);
      border-color: rgba(148, 163, 184, 0.2);
      color: rgba(245, 250, 255, 0.94);
    }
  }

  .cookie-error {
    background: rgba(80, 18, 18, 0.36);
    border-color: rgba(248, 113, 113, 0.28);
  }
}
</style>
