<!--
SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
SPDX-License-Identifier: Apache-2.0

This file is part of the Android port in this repository.
Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
for Android integration, mobile UX, and distribution compliance.
See the repository LICENSE and NOTICE files for details.
-->

<template>
  <el-container class="main-layout" :class="{ 'is-mobile': isMobile }">
    <!-- PC端侧边栏 -->
    <el-aside v-if="!isMobile" :width="isCollapse ? '64px' : '200px'" class="sidebar">
      <div class="logo">
        <el-icon :size="32" color="#409eff">
          <FolderOpened />
        </el-icon>
        <transition name="fade">
          <span v-if="!isCollapse" class="logo-text">{{ APP_DISPLAY_NAME }}</span>
        </transition>
      </div>

      <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :collapse-transition="false"
          class="sidebar-menu"
          router
      >
        <el-menu-item index="/files">
          <el-icon><Files /></el-icon>
          <template #title>文件管理</template>
        </el-menu-item>

        <el-menu-item index="/downloads">
          <el-icon><Download /></el-icon>
          <template #title>下载管理</template>
        </el-menu-item>

        <el-menu-item index="/uploads">
          <el-icon><Upload /></el-icon>
          <template #title>上传管理</template>
        </el-menu-item>

        <el-menu-item index="/share-transfer">
          <el-icon><Share /></el-icon>
          <template #title>分享与转存</template>
        </el-menu-item>

        <el-menu-item index="/cloud-dl">
          <el-icon><Link /></el-icon>
          <template #title>离线下载</template>
        </el-menu-item>

        <el-menu-item index="/settings">
          <el-icon><Setting /></el-icon>
          <template #title>系统设置</template>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <el-button
            :icon="isCollapse ? Expand : Fold"
            circle
            @click="toggleCollapse"
        />
      </div>
    </el-aside>

    <!-- 移动端抽屉导航 -->
    <teleport to="body">
      <transition name="drawer-fade">
        <div
            v-if="drawerVisible"
            class="mobile-drawer-overlay"
            role="presentation"
            @click.self="closeDrawer"
        >
          <transition name="drawer-slide" appear>
            <aside class="mobile-drawer-panel" role="navigation" aria-label="移动端导航">
              <div class="drawer-content">
                <div class="drawer-logo">
                  <el-icon :size="32" color="#409eff">
                    <FolderOpened />
                  </el-icon>
                  <span>{{ APP_DISPLAY_NAME }}</span>
                </div>

                <el-menu
                    :default-active="activeMenu"
                    :collapse-transition="false"
                    class="drawer-menu"
                    router
                    @select="handleMenuSelect"
                >
                  <el-menu-item index="/files">
                    <el-icon><Files /></el-icon>
                    <span>文件管理</span>
                  </el-menu-item>

                  <el-menu-item index="/downloads">
                    <el-icon><Download /></el-icon>
                    <span>下载管理</span>
                  </el-menu-item>

                  <el-menu-item index="/uploads">
                    <el-icon><Upload /></el-icon>
                    <span>上传管理</span>
                  </el-menu-item>

                  <el-menu-item index="/share-transfer">
                    <el-icon><Share /></el-icon>
                    <span>分享与转存</span>
                  </el-menu-item>

                  <el-menu-item index="/cloud-dl">
                    <el-icon><Link /></el-icon>
                    <span>离线下载</span>
                  </el-menu-item>

                  <el-menu-item index="/settings">
                    <el-icon><Setting /></el-icon>
                    <span>系统设置</span>
                  </el-menu-item>
                </el-menu>

                <!-- 抽屉底部用户信息 -->
                <div class="drawer-footer">
                  <div class="drawer-user" @click="handleUserClick">
                    <el-avatar :size="36" :src="userAvatar">
                      <el-icon><User /></el-icon>
                    </el-avatar>
                    <span class="drawer-username">{{ username }}</span>
                  </div>
                  <div class="drawer-logout-buttons">
                    <el-button type="danger" plain size="small" @click="handleDrawerLogout">
                      <el-icon><SwitchButton /></el-icon>
                      退出百度
                    </el-button>
                    <el-button v-if="webAuthStore.isAuthEnabled" type="warning" plain size="small" @click="handleDrawerWebLogout">
                      <el-icon><Lock /></el-icon>
                      退出Web
                    </el-button>
                  </div>
                </div>
              </div>
            </aside>
          </transition>
        </div>
      </transition>
    </teleport>

    <!-- 主内容区 -->
    <el-container class="main-container">
      <!-- 顶部栏 -->
      <el-header
          :height="isMobile ? 'calc(var(--app-mobile-header-height) + var(--app-mobile-safe-top))' : '56px'"
          class="top-header"
          :class="{ 'is-mobile': isMobile }"
      >
        <div v-if="isMobile" class="header-safe-area" />
        <div class="header-bar" :class="{ 'is-mobile': isMobile }">
          <div class="header-left">
            <!-- 移动端菜单/返回按钮 -->
            <el-button
                v-if="isMobile"
                :icon="showMobileBackButton ? ArrowLeft : Menu"
                circle
                class="mobile-menu-btn"
                @click="handleMobileLeadingAction"
            />
            <h3>{{ pageTitle }}</h3>
          </div>

          <div class="header-right">
            <el-dropdown @command="handleCommand">
              <div class="user-info">
                <el-avatar :size="32" :src="userAvatar">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <span v-if="!isMobile" class="username">{{ username }}</span>
                <el-icon><CaretBottom /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>
                    个人信息
                  </el-dropdown-item>
                  <el-dropdown-item command="credits">
                    <el-icon><InfoFilled /></el-icon>
                    {{ APP_LEGAL_PAGE_NAME }}
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided>
                    <el-icon><SwitchButton /></el-icon>
                    退出百度账号
                  </el-dropdown-item>
                  <el-dropdown-item v-if="webAuthStore.isAuthEnabled" command="webLogout">
                    <el-icon><Lock /></el-icon>
                    退出 Web 认证
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main-content" :class="{ 'has-tabbar': showMobileTabbar }">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>

    <!-- 移动端底部导航栏 -->
    <div v-if="showMobileTabbar" class="mobile-tabbar">
      <div
          v-for="item in tabbarItems"
          :key="item.path"
          class="tabbar-item"
          :class="{ active: activeMenu === item.path }"
          @click="navigateTo(item.path)"
      >
        <el-icon :size="22">
          <component :is="item.icon" />
        </el-icon>
        <span class="tabbar-label">{{ item.label }}</span>
      </div>
    </div>

    <!-- 个人信息弹窗 -->
    <UserProfileDialog v-model="profileDialogVisible" :user="authStore.user" />
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, watch, markRaw } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useWebAuthStore } from '@/stores/webAuth'
import { useIsMobile } from '@/utils/responsive'
import { APP_DISPLAY_NAME, APP_LEGAL_PAGE_NAME } from '@/constants/appInfo'
import UserProfileDialog from '@/components/UserProfileDialog.vue'
import {
  FolderOpened,
  Files,
  Download,
  Upload,
  Setting,
  User,
  CaretBottom,
  SwitchButton,
  Expand,
  Fold,
  Share,
  Menu,
  Lock,
  Link,
  ArrowLeft,
  InfoFilled,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const webAuthStore = useWebAuthStore()

// 响应式检测
const isMobile = useIsMobile()

// 状态
const isCollapse = ref(false)
const drawerVisible = ref(false)
const profileDialogVisible = ref(false)

// 底部导航栏配置
const tabbarItems = [
  { path: '/files', label: '文件', icon: markRaw(Files) },
  { path: '/downloads', label: '下载', icon: markRaw(Download) },
  { path: '/uploads', label: '上传', icon: markRaw(Upload) },
  { path: '/share-transfer', label: '分享', icon: markRaw(Share) },
  { path: '/settings', label: '设置', icon: markRaw(Setting) },
]

// 计算属性
const activeMenu = computed(() => route.path)
const username = computed(() => authStore.username || '未登录')
const userAvatar = computed(() => authStore.avatar)
const showMobileBackButton = computed(() => isMobile.value && route.meta.mobileSubpage === true)
const showMobileTabbar = computed(() => isMobile.value && route.meta.hideMobileTabbar !== true)

const pageTitle = computed(() => {
  const titles: Record<string, string> = {
    '/files': '文件管理',
    '/downloads': '下载管理',
    '/uploads': '上传管理',
    '/share-transfer': '分享与转存',
    '/cloud-dl': '离线下载',
    '/settings': '系统设置',
    '/about/credits': APP_LEGAL_PAGE_NAME,
  }
  return titles[route.path] || APP_DISPLAY_NAME
})

// 切换侧边栏折叠状态
function toggleCollapse() {
  isCollapse.value = !isCollapse.value
}

// 底部导航栏点击
function navigateTo(path: string) {
  router.push(path)
}

function handleMobileLeadingAction() {
  if (showMobileBackButton.value) {
    if (window.history.length > 1) {
      router.back()
    } else {
      router.push('/files')
    }
    return
  }

  drawerVisible.value = true
}

function closeDrawer() {
  drawerVisible.value = false
}

// 抽屉菜单选择（关闭抽屉）
function handleMenuSelect() {
  drawerVisible.value = false
}

// 抽屉用户点击
function handleUserClick() {
  drawerVisible.value = false
  profileDialogVisible.value = true
}

// 抽屉退出百度账号
async function handleDrawerLogout() {
  drawerVisible.value = false
  await handleLogout()
}

// 抽屉退出 Web 认证
async function handleDrawerWebLogout() {
  drawerVisible.value = false
  await handleWebLogout()
}

// 抽屉退出登录
async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    drawerVisible.value = false
    await authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('退出登录失败:', error)
    }
  }
}

// 下拉菜单命令处理
async function handleCommand(command: string) {
  switch (command) {
    case 'profile':
      profileDialogVisible.value = true
      break
    case 'credits':
      router.push('/about/credits')
      break
    case 'logout':
      await handleLogout()
      break
    case 'webLogout':
      await handleWebLogout()
      break
  }
}

// Web 认证登出
async function handleWebLogout() {
  try {
    await ElMessageBox.confirm('确定要退出 Web 认证吗？退出后需要重新登录才能访问。', '退出确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await webAuthStore.logout()
    ElMessage.success('已退出 Web 认证')
    router.push('/web-login')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('退出 Web 认证失败:', error)
    }
  }
}

// 监听路由变化，移动端自动关闭抽屉
watch(
    () => route.path,
    () => {
      if (isMobile.value) {
        drawerVisible.value = false
      }
    }
)
</script>

<style scoped lang="scss">
.main-layout {
  width: 100%;
  height: 100vh;
  height: 100dvh;
  display: flex;
  flex-direction: row;
  background: transparent;

  &.is-mobile {
    flex-direction: column;
  }
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.sidebar {
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #11202d 0%, #162a39 100%);
  transition: width 0.3s;
  overflow: hidden;
  flex-shrink: 0;

  .logo {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    height: 60px;
    padding: 0 20px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);

    .logo-text {
      font-size: 18px;
      font-weight: 600;
      color: white;
      white-space: nowrap;
    }
  }

  .sidebar-menu {
    flex: 1;
    border-right: none;
    background: transparent;

    :deep(.el-menu-item) {
      color: rgba(255, 255, 255, 0.7);

      &:hover {
        background-color: rgba(255, 255, 255, 0.1) !important;
        color: white;
      }

      &.is-active {
        background-color: #409eff !important;
        color: white;
      }
    }
  }

  .sidebar-footer {
    display: flex;
    justify-content: center;
    padding: 20px 0;
    border-top: 1px solid rgba(255, 255, 255, 0.1);
  }
}

.top-header {
  background: var(--app-surface-overlay);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid var(--app-border);
  flex-shrink: 0;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.04);
  padding: 0;

  .header-safe-area {
    height: var(--app-mobile-safe-top);
    flex-shrink: 0;
  }

  .header-bar {
    min-height: 56px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 20px;

    &.is-mobile {
      min-height: var(--app-mobile-header-height);
      padding: 0 12px;
    }
  }

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
    flex-shrink: 0;

    .mobile-menu-btn {
      flex-shrink: 0;
    }

    h3 {
      margin: 0;
      font-size: 17px;
      color: var(--app-text);
      white-space: nowrap;
    }
  }

  .header-right {
    flex-shrink: 0;

    .user-info {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      cursor: pointer;
      padding: 6px 12px;
      min-width: var(--app-mobile-touch-target);
      min-height: var(--app-mobile-touch-target);
      border-radius: 999px;
      transition: background-color 0.2s;
      color: var(--app-text);

      &:hover {
        background-color: var(--app-accent-soft);
      }

      .username {
        font-size: 14px;
        color: var(--app-text);
      }
    }
  }
}

.main-content {
  padding: 0;
  background: transparent;
  overflow: hidden;
  flex: 1;
  min-height: 0;

  // 移动端有底部导航栏时，增加底部内边距
  &.has-tabbar {
    padding-bottom: calc(var(--app-tabbar-height) + env(safe-area-inset-bottom, 0));
  }
}

// =====================
// 移动端抽屉导航样式
// =====================
.mobile-drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 2600;
  background: rgba(2, 8, 14, 0.46);
  backdrop-filter: blur(6px);
}

.mobile-drawer-panel {
  width: min(78vw, 280px);
  height: 100%;
  padding-top: env(safe-area-inset-top, 0);
  background: linear-gradient(180deg, #11202d 0%, #162a39 100%);
  box-shadow: 18px 0 44px rgba(0, 0, 0, 0.28);
  touch-action: pan-y;
}

.drawer-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: linear-gradient(180deg, #11202d 0%, #162a39 100%);

  .drawer-logo {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    height: 60px;
    padding: 0 20px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    color: white;
    font-size: 18px;
    font-weight: 600;
  }

  .drawer-menu {
    flex: 1;
    border-right: none;
    background: transparent;

    :deep(.el-menu-item) {
      height: 56px;
      line-height: 56px;
      color: rgba(255, 255, 255, 0.7);
      font-size: 15px;

      .el-icon {
        margin-right: 12px;
      }

      &:hover {
        background-color: rgba(255, 255, 255, 0.1) !important;
        color: white;
      }

      &.is-active {
        background-color: #409eff !important;
        color: white;
      }
    }
  }

  .drawer-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 20px;
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    background: rgba(0, 0, 0, 0.1);

    .drawer-user {
      display: flex;
      align-items: center;
      gap: 12px;
      cursor: pointer;

      .drawer-username {
        color: white;
        font-size: 14px;
      }
    }

    .drawer-logout-buttons {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
  }
}

// =====================
// 移动端底部导航栏样式
// =====================
.mobile-tabbar {
  display: flex;
  justify-content: space-around;
  align-items: center;
  min-height: var(--app-tabbar-height);
  background: var(--app-surface-overlay);
  border-top: 1px solid var(--app-border);
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  // iOS 安全区域适配
  padding-bottom: env(safe-area-inset-bottom, 0);
  backdrop-filter: blur(18px);

  .tabbar-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    flex: 1;
    height: 100%;
    padding: 6px 0;
    color: var(--app-text-secondary);
    cursor: pointer;
    transition: color 0.2s;
    // 最小触摸区域 44x44
    min-width: 56px;
    min-height: 48px;

    // 触摸反馈
    &:active {
      background-color: var(--app-accent-soft);
    }

    &.active {
      color: var(--app-accent);

      .tabbar-label {
        font-weight: 600;
      }
    }

    .tabbar-label {
      font-size: 11px;
      margin-top: 2px;
    }
  }
}

// 移动端内容区底部留白（为底部导航栏留空间）
.is-mobile {
  .main-content {
    min-height: 0;
  }
}

// =====================
// 过渡动画
// =====================
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.2s;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(10px);
}

.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity 0.2s ease;
}

.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;
}

.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: transform 0.24s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.drawer-slide-enter-from,
.drawer-slide-leave-to {
  transform: translateX(-102%);
}

// =====================
// 移动端响应式调整
// =====================
@media (max-width: 767px) {
  .top-header {
    .header-left {
      h3 {
        font-size: 15px;
      }
    }
  }
}
</style>
