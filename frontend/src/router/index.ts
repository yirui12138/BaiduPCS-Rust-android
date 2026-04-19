// SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
// SPDX-License-Identifier: Apache-2.0
//
// This file is part of the Android port in this repository.
// Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
// for Android integration, mobile UX, and distribution compliance.
// See the repository LICENSE and NOTICE files for details.

import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useWebAuthStore } from '@/stores/webAuth'
import { APP_DISPLAY_NAME, APP_LEGAL_PAGE_NAME } from '@/constants/appInfo'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', requiresBaiduAuth: true },
  },
  {
    path: '/web-login',
    name: 'WebLogin',
    component: () => import('@/views/WebLoginView.vue'),
    meta: { title: 'Web 认证登录', skipWebAuth: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '/files',
        name: 'Files',
        component: () => import('@/views/FilesView.vue'),
        meta: { title: '文件管理' },
      },
      {
        path: '/downloads',
        name: 'Downloads',
        component: () => import('@/views/DownloadsView.vue'),
        meta: { title: '下载管理' },
      },
      {
        path: '/uploads',
        name: 'Uploads',
        component: () => import('@/views/UploadsView.vue'),
        meta: { title: '上传管理' },
      },
      {
        path: '/transfers',
        redirect: { path: '/share-transfer', query: { tab: 'transfers' } },
      },
      {
        path: '/autobackup',
        redirect: '/files',
      },
      {
        path: '/cloud-dl',
        name: 'CloudDl',
        component: () => import('@/views/CloudDlView.vue'),
        meta: { title: '离线下载' },
      },
      {
        path: '/shares',
        redirect: { path: '/share-transfer', query: { tab: 'shares' } },
      },
      {
        path: '/share-transfer',
        name: 'ShareTransfer',
        component: () => import('@/views/ShareTransferView.vue'),
        meta: { title: '分享与转存' },
      },
      {
        path: '/settings',
        name: 'Settings',
        component: () => import('@/views/SettingsView.vue'),
        meta: { title: '系统设置' },
      },
      {
        path: '/about/credits',
        name: 'Credits',
        component: () => import('@/views/CreditsView.vue'),
        meta: {
          title: APP_LEGAL_PAGE_NAME,
          hideMobileTabbar: true,
          mobileSubpage: true,
        },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

let webAuthInitialized = false

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()
  const webAuthStore = useWebAuthStore()

  if (to.meta.title) {
    document.title = `${to.meta.title} - ${APP_DISPLAY_NAME}`
  }

  if (to.meta.skipWebAuth) {
    next()
    return
  }

  if (!webAuthInitialized) {
    webAuthInitialized = true
    webAuthStore.initialize().then(() => {
      webAuthStore.checkAuthStatus().catch((error) => {
        console.error('获取 Web 认证状态失败:', error)
      })
    })
  }

  if (webAuthStore.authConfig && webAuthStore.isAuthEnabled && !webAuthStore.isAuthenticated) {
    next('/web-login')
    return
  }

  const needsBaiduAuth = Boolean(to.meta.requiresAuth || to.meta.requiresBaiduAuth || to.path === '/login')
  const hasBaiduSession = needsBaiduAuth
    ? (authStore.isLoggedIn || await authStore.ensureSession())
    : authStore.isLoggedIn

  if ((to.meta.requiresBaiduAuth || to.path === '/login') && hasBaiduSession) {
    next('/files')
    return
  }

  if (to.meta.requiresAuth && !hasBaiduSession) {
    next('/login')
    return
  }

  next()
})

export default router
