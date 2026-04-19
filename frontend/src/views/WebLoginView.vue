<!--
SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
SPDX-License-Identifier: Apache-2.0

This file is part of the Android port in this repository.
Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
for Android integration, mobile UX, and distribution compliance.
See the repository LICENSE and NOTICE files for details.
-->

<template>
  <div class="web-login-container" :class="{ 'is-mobile': isMobile }">
    <div class="login-card">
      <!-- Logo 和标题 -->
      <div class="header">
        <div class="logo">
          <el-icon :size="isMobile ? 40 : 48" color="#409eff">
            <Lock />
          </el-icon>
        </div>
        <h1>Web 访问认证</h1>
        <p class="subtitle">{{ stepSubtitle }}</p>
      </div>

      <!-- 登录表单 -->
      <div class="login-form">
        <!-- 密码输入步骤 -->
        <div v-if="showPasswordStep" class="form-step">
          <el-form @submit.prevent="handlePasswordSubmit">
            <el-form-item>
              <el-input
                ref="passwordInputRef"
                v-model="password"
                type="password"
                placeholder="请输入访问密码"
                size="large"
                show-password
                :disabled="isLoading || isLocked"
                @keyup.enter="handlePasswordSubmit"
                autocomplete="current-password"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-button
              type="primary"
              size="large"
              :loading="isLoading"
              :disabled="isLocked"
              @click="handlePasswordSubmit"
              class="submit-btn"
            >
              {{ requiresOnlyPassword ? '登录' : '下一步' }}
            </el-button>
          </el-form>
        </div>

        <!-- TOTP 输入步骤 -->
        <div v-if="showTotpStep" class="form-step">
          <!-- 返回按钮（仅在两步验证时显示） -->
          <div v-if="loginStep === 'totp' && requiresPassword" class="back-link">
            <el-button link type="info" @click="handleBackToPassword">
              <el-icon><ArrowLeft /></el-icon>
              返回密码输入
            </el-button>
          </div>

          <el-form @submit.prevent="handleTotpSubmit">
            <el-form-item>
              <el-input
                ref="totpInputRef"
                v-model="totpCode"
                placeholder="请输入 6 位验证码"
                size="large"
                maxlength="6"
                :disabled="isLoading || isLocked"
                @keyup.enter="handleTotpSubmit"
                inputmode="numeric"
                pattern="[0-9]*"
                autocomplete="one-time-code"
              >
                <template #prefix>
                  <el-icon><Key /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-button
              type="primary"
              size="large"
              :loading="isLoading"
              :disabled="isLocked"
              @click="handleTotpSubmit"
              class="submit-btn"
            >
              验证
            </el-button>

            <!-- 恢复码入口 -->
            <div class="recovery-link">
              <el-button link type="primary" @click="showRecoveryInput = true">
                <el-icon><Ticket /></el-icon>
                使用恢复码登录
              </el-button>
            </div>
          </el-form>
        </div>

        <!-- 恢复码输入（移动端使用抽屉，桌面端使用对话框） -->
        <el-drawer
          v-if="isMobile"
          v-model="showRecoveryInput"
          title="使用恢复码登录"
          direction="btt"
          size="auto"
          :close-on-click-modal="true"
        >
          <div class="recovery-drawer-content">
            <p class="recovery-hint">请输入您保存的恢复码，每个恢复码只能使用一次</p>
            <el-form @submit.prevent="handleRecoverySubmit">
              <el-form-item>
                <el-input
                  ref="recoveryInputRef"
                  v-model="recoveryCode"
                  placeholder="XXXX-XXXX"
                  size="large"
                  :disabled="isLoading"
                  autocomplete="off"
                >
                  <template #prefix>
                    <el-icon><Ticket /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
              <div class="recovery-actions">
                <el-button size="large" @click="showRecoveryInput = false">取消</el-button>
                <el-button type="primary" size="large" :loading="isLoading" @click="handleRecoverySubmit">
                  验证
                </el-button>
              </div>
            </el-form>
          </div>
        </el-drawer>

        <el-dialog
          v-else
          v-model="showRecoveryInput"
          title="使用恢复码登录"
          width="400px"
          :close-on-click-modal="false"
        >
          <p class="recovery-hint">请输入您保存的恢复码，每个恢复码只能使用一次</p>
          <el-form @submit.prevent="handleRecoverySubmit">
            <el-form-item>
              <el-input
                ref="recoveryInputRef"
                v-model="recoveryCode"
                placeholder="XXXX-XXXX"
                size="large"
                :disabled="isLoading"
                autocomplete="off"
              >
                <template #prefix>
                  <el-icon><Ticket /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="showRecoveryInput = false">取消</el-button>
            <el-button type="primary" :loading="isLoading" @click="handleRecoverySubmit">
              验证
            </el-button>
          </template>
        </el-dialog>

        <!-- 错误提示 -->
        <transition name="fade">
          <div v-if="error" class="error-message">
            <el-alert :title="error" type="error" :closable="true" show-icon @close="clearError" />
          </div>
        </transition>

        <!-- 速率限制提示 -->
        <transition name="fade">
          <div v-if="lockoutRemaining && lockoutRemaining > 0" class="lockout-message">
            <el-alert
              :title="`请求过于频繁，请在 ${lockoutRemaining} 秒后重试`"
              type="warning"
              :closable="false"
              show-icon
            />
            <div class="lockout-countdown">
              <el-progress
                :percentage="lockoutProgress"
                :show-text="false"
                :stroke-width="4"
                status="warning"
              />
            </div>
          </div>
        </transition>
      </div>

      <!-- 底部信息 -->
      <div class="footer">
        <p>基于 Rust + Axum + Vue 3 构建</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useWebAuthStore } from '@/stores/webAuth'
import { useIsMobile } from '@/utils/responsive'
import { Lock, Key, Ticket, ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()
const webAuthStore = useWebAuthStore()
const isMobile = useIsMobile()

// 输入框引用
const passwordInputRef = ref<InstanceType<typeof import('element-plus')['ElInput']> | null>(null)
const totpInputRef = ref<InstanceType<typeof import('element-plus')['ElInput']> | null>(null)
const recoveryInputRef = ref<InstanceType<typeof import('element-plus')['ElInput']> | null>(null)

// 表单状态
const password = ref('')
const totpCode = ref('')
const recoveryCode = ref('')
const showRecoveryInput = ref(false)

// 速率限制倒计时
const lockoutCountdown = ref<number | null>(null)
let lockoutTimer: ReturnType<typeof setInterval> | null = null

// 从 store 获取状态
const loginStep = computed(() => webAuthStore.loginStep)
const isLoading = computed(() => webAuthStore.isLoading)
const error = computed(() => webAuthStore.error)
const lockoutRemaining = computed(() => lockoutCountdown.value ?? webAuthStore.lockoutRemaining)
const requiresPassword = computed(() => webAuthStore.requiresPassword)
const requiresTotp = computed(() => webAuthStore.requiresTotp)
const requiresOnlyPassword = computed(() => requiresPassword.value && !requiresTotp.value)
const requiresOnlyTotp = computed(() => !requiresPassword.value && requiresTotp.value)

// 是否被锁定
const isLocked = computed(() => lockoutRemaining.value !== null && lockoutRemaining.value > 0)

// 锁定进度（用于进度条）
const lockoutProgress = computed(() => {
  if (!lockoutRemaining.value) return 0
  // 假设最大锁定时间为 15 分钟 (900 秒)
  const maxLockout = 900
  return Math.max(0, Math.min(100, (lockoutRemaining.value / maxLockout) * 100))
})

// 显示密码步骤
const showPasswordStep = computed(() => {
  return loginStep.value === 'password' && requiresPassword.value
})

// 显示 TOTP 步骤
const showTotpStep = computed(() => {
  // 两步验证的第二步
  if (loginStep.value === 'totp') return true
  // 仅 TOTP 模式
  if (loginStep.value === 'password' && requiresOnlyTotp.value) return true
  return false
})

// 步骤副标题
const stepSubtitle = computed(() => {
  if (showPasswordStep.value) {
    return '请输入访问密码'
  }
  if (showTotpStep.value) {
    if (loginStep.value === 'totp') {
      return '请输入双因素验证码'
    }
    return '请输入验证码以访问系统'
  }
  return '请输入凭证以访问系统'
})

// 清除错误
function clearError() {
  // 直接设置 store 的 error ref
  webAuthStore.$patch({ error: null })
}

// 开始锁定倒计时
function startLockoutCountdown(seconds: number) {
  stopLockoutCountdown()
  lockoutCountdown.value = seconds
  
  lockoutTimer = setInterval(() => {
    if (lockoutCountdown.value !== null && lockoutCountdown.value > 0) {
      lockoutCountdown.value--
    } else {
      stopLockoutCountdown()
    }
  }, 1000)
}

// 停止锁定倒计时
function stopLockoutCountdown() {
  if (lockoutTimer) {
    clearInterval(lockoutTimer)
    lockoutTimer = null
  }
  lockoutCountdown.value = null
}

// 返回密码输入
function handleBackToPassword() {
  webAuthStore.resetLoginFlow()
  password.value = ''
  totpCode.value = ''
  nextTick(() => {
    passwordInputRef.value?.focus()
  })
}

// 密码提交
async function handlePasswordSubmit() {
  if (!password.value) {
    ElMessage.warning('请输入密码')
    return
  }

  if (isLocked.value) {
    return
  }

  try {
    const response = await webAuthStore.loginWithPassword(password.value)
    if (response.status === 'success') {
      ElMessage.success('登录成功')
      router.push('/login')
    } else if (response.status === 'need_totp') {
      // 清空密码，聚焦 TOTP 输入框
      nextTick(() => {
        totpInputRef.value?.focus()
      })
    } else if (response.lockout_remaining) {
      startLockoutCountdown(response.lockout_remaining)
    }
  } catch (err: any) {
    // 检查是否有锁定时间
    const lockout = err.response?.data?.details?.lockout_remaining
    if (lockout) {
      startLockoutCountdown(lockout)
    }
  }
}

// TOTP 提交
async function handleTotpSubmit() {
  if (!totpCode.value || totpCode.value.length !== 6) {
    ElMessage.warning('请输入 6 位验证码')
    return
  }

  if (isLocked.value) {
    return
  }

  try {
    const response = await webAuthStore.verifyTotp(totpCode.value)
    if (response.status === 'success') {
      ElMessage.success('登录成功')
      router.push('/login')
    }
  } catch (err) {
    // 错误已在 store 中处理
    totpCode.value = ''
  }
}

// 恢复码提交
async function handleRecoverySubmit() {
  if (!recoveryCode.value) {
    ElMessage.warning('请输入恢复码')
    return
  }

  try {
    const response = await webAuthStore.loginWithRecoveryCode(recoveryCode.value)
    if (response.status === 'success') {
      ElMessage.success('登录成功')
      showRecoveryInput.value = false
      router.push('/login')
    }
  } catch (err) {
    // 错误已在 store 中处理
    recoveryCode.value = ''
  }
}

// 监听认证状态变化
watch(() => webAuthStore.isAuthenticated, (isAuth) => {
  if (isAuth) {
    router.push('/login')
  }
})

// 监听恢复码对话框打开
watch(showRecoveryInput, (show) => {
  if (show) {
    nextTick(() => {
      recoveryInputRef.value?.focus()
    })
  } else {
    recoveryCode.value = ''
  }
})

// 监听 store 的 lockoutRemaining 变化
watch(() => webAuthStore.lockoutRemaining, (newVal) => {
  if (newVal && newVal > 0) {
    startLockoutCountdown(newVal)
  }
})

// 组件挂载
onMounted(async () => {
  // 初始化 store
  webAuthStore.initialize()
  
  // 检查认证状态
  try {
    await webAuthStore.checkAuthStatus()
    
    // 如果认证未启用或已认证，跳转到登录页
    if (!webAuthStore.isAuthEnabled || webAuthStore.isAuthenticated) {
      router.push('/login')
      return
    }
  } catch (err) {
    console.error('获取认证状态失败:', err)
  }

  // 重置登录流程
  webAuthStore.resetLoginFlow()
  
  // 聚焦第一个输入框
  nextTick(() => {
    if (showPasswordStep.value) {
      passwordInputRef.value?.focus()
    } else if (showTotpStep.value) {
      totpInputRef.value?.focus()
    }
  })
})

// 组件卸载
onUnmounted(() => {
  stopLockoutCountdown()
})
</script>

<style scoped lang="scss">
.web-login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  min-height: 100dvh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.header {
  text-align: center;
  margin-bottom: 32px;

  .logo {
    margin-bottom: 16px;
  }

  h1 {
    margin: 0 0 8px 0;
    font-size: 24px;
    font-weight: 600;
    color: #333;
  }

  .subtitle {
    margin: 0;
    font-size: 14px;
    color: #999;
    transition: all 0.3s ease;
  }
}

.login-form {
  .form-step {
    margin-bottom: 20px;
  }

  .back-link {
    margin-bottom: 16px;
    
    .el-button {
      padding: 0;
      font-size: 14px;
      
      .el-icon {
        margin-right: 4px;
      }
    }
  }

  .submit-btn {
    width: 100%;
    margin-top: 8px;
    height: 44px;
    font-size: 16px;
  }

  .recovery-link {
    text-align: center;
    margin-top: 16px;
    
    .el-button {
      font-size: 14px;
      
      .el-icon {
        margin-right: 4px;
      }
    }
  }

  .error-message,
  .lockout-message {
    margin-top: 16px;
  }

  .lockout-countdown {
    margin-top: 8px;
  }
}

.recovery-hint {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: #666;
  line-height: 1.5;
}

.recovery-drawer-content {
  padding: 0 4px;
  
  .recovery-actions {
    display: flex;
    gap: 12px;
    margin-top: 16px;
    
    .el-button {
      flex: 1;
    }
  }
}

.footer {
  margin-top: 32px;
  text-align: center;
  font-size: 12px;
  color: #999;

  p {
    margin: 0;
  }
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.web-login-container.is-mobile {
  padding:
    max(16px, env(safe-area-inset-top, 0px))
    16px
    calc(24px + env(safe-area-inset-bottom, 0px));
  align-items: flex-start;

  .login-card {
    padding: 24px 20px;
    border-radius: 12px;
  }

  .header {
    margin-bottom: 24px;

    h1 {
      font-size: 20px;
    }

    .subtitle {
      font-size: 13px;
    }
  }

  .login-form {
    .submit-btn {
      height: 48px;
      font-size: 16px;
    }

    :deep(.el-input__wrapper) {
      padding: 8px 12px;
    }

    :deep(.el-input__inner) {
      font-size: 16px;
      height: 28px;
    }
  }

  .recovery-link {
    margin-top: 20px;
    
    .el-button {
      font-size: 15px;
      padding: 8px 0;
    }
  }
}

/* 小屏幕手机适配 */
@media (max-width: 375px) {
  .web-login-container {
    padding:
      max(12px, env(safe-area-inset-top, 0px))
      12px
      calc(20px + env(safe-area-inset-bottom, 0px));
  }

  .login-card {
    padding: 20px 16px;
  }

  .header {
    margin-bottom: 20px;

    h1 {
      font-size: 18px;
    }

    .subtitle {
      font-size: 12px;
    }
  }
}

/* 横屏适配 */
@media (max-width: 768px) and (orientation: landscape) {
  .web-login-container {
    padding-top: 20px;
    padding-bottom: 20px;
    align-items: center;
  }

  .login-card {
    padding: 20px 24px;
    max-width: 480px;
  }

  .header {
    margin-bottom: 16px;
    
    .logo {
      margin-bottom: 8px;
    }
  }

  .footer {
    margin-top: 16px;
  }
}

/* 触摸友好的交互 */
@media (hover: none) and (pointer: coarse) {
  .login-form {
    .submit-btn {
      /* 增大按钮触摸区域 */
      min-height: 48px;
    }

    .recovery-link .el-button {
      /* 增大链接触摸区域 */
      padding: 12px 16px;
      margin: -8px -16px;
    }

    .back-link .el-button {
      padding: 8px 12px;
      margin: -4px -12px;
    }
  }
}

/* 深色模式支持（可选） */
@media (prefers-color-scheme: dark) {
  .web-login-container {
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  }
}
</style>
