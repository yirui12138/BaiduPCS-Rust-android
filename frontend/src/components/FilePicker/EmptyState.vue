<!--
SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
SPDX-License-Identifier: Apache-2.0

This file is part of the Android port in this repository.
Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
for Android integration, mobile UX, and distribution compliance.
See the repository LICENSE and NOTICE files for details.
-->

<template>
  <div class="empty-state">
    <el-empty description="此文件夹为空" :image-size="100">
      <template #image>
        <el-icon class="empty-icon"><FolderOpened /></el-icon>
      </template>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { FolderOpened } from '@element-plus/icons-vue'
</script>

<style scoped>
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 200px;
}

.empty-icon {
  font-size: 64px;
  color: var(--el-text-color-secondary);
  opacity: 0.5;
}
</style>