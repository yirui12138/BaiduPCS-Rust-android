<!--
SPDX-FileCopyrightText: Copyright 2026 Android Port Contributors
SPDX-License-Identifier: Apache-2.0

This file is part of the Android port in this repository.
Derived from BaiduPCS-Rust v1.12.1 by komorebiCarry and modified
for Android integration, mobile UX, and distribution compliance.
See the repository LICENSE and NOTICE files for details.
-->

<template>
  <div class="settings-container" :class="{ 'is-mobile': isMobile }">
    <el-container>
      <!-- 顶部标题 -->
      <el-header height="60px" class="header">
        <h2 v-if="!isMobile">系统设置</h2>
        <div class="header-actions">
          <template v-if="!isMobile">
            <el-button @click="handleReset" :loading="resetting">
              <el-icon><RefreshLeft /></el-icon>
              恢复推荐配置
            </el-button>
            <el-button type="primary" @click="handleSave" :loading="saving">
              <el-icon><Check /></el-icon>
              保存设置
            </el-button>
          </template>
          <template v-else>
            <el-button circle @click="handleReset" :loading="resetting">
              <el-icon><RefreshLeft /></el-icon>
            </el-button>
            <el-button type="primary" circle @click="handleSave" :loading="saving">
              <el-icon><Check /></el-icon>
            </el-button>
          </template>
        </div>
      </el-header>

      <!-- 设置内容 -->
      <el-main>
        <div class="settings-layout">
          <!-- 左侧锚点导航（仅桌面端） -->
          <nav v-if="!isMobile" class="settings-nav">
            <ul>
              <li
                  v-for="item in navItems"
                  :key="item.id"
                  :class="{ active: activeSection === item.id }"
                  @click="scrollToSection(item.id)"
              >
                <span class="nav-dot" :style="{ background: item.color }"></span>
                <span class="nav-label">{{ item.label }}</span>
              </li>
            </ul>
          </nav>

          <!-- 右侧内容区 -->
          <div class="settings-content" ref="contentRef">
            <el-skeleton :loading="loading" :rows="8" animated>
              <el-form
                  v-if="formData"
                  ref="formRef"
                  :model="formData"
                  :rules="rules"
                  label-width="140px"
                  label-position="left"
              >
                <!-- 服务器配置 -->
                <CollapsibleSettingCard
                    id="section-server"
                    title="服务器配置"
                    description="监听地址与端口"
                    color="#409eff"
                    :expanded="isSectionExpanded('section-server')"
                    @update:expanded="toggleSection('section-server', $event)"
                >
                  <template #icon>
                    <el-icon><Monitor /></el-icon>
                  </template>
                  <template #header>
                    <div class="card-header">
                      <el-icon :size="20" color="#409eff">
                        <Monitor />
                      </el-icon>
                      <span>服务器配置</span>
                    </div>
                  </template>

                  <el-form-item label="监听地址" prop="server.host">
                    <el-input
                        v-model="formData.server.host"
                        placeholder="例如: 127.0.0.1"
                        clearable
                    >
                      <template #prepend>
                        <el-icon><Connection /></el-icon>
                      </template>
                    </el-input>
                    <div class="form-tip">服务器监听的IP地址</div>
                  </el-form-item>

                  <el-form-item label="监听端口" prop="server.port">
                    <el-input-number
                        v-model="formData.server.port"
                        :min="1"
                        :max="65535"
                        :step="1"
                        controls-position="right"
                        style="width: 100%"
                    />
                    <div class="form-tip">服务器监听的端口号，修改后需要重启服务器</div>
                  </el-form-item>
                </CollapsibleSettingCard>

                <!-- Web 访问认证设置 -->
                <CollapsibleSettingCard
                    id="section-auth"
                    title="Web 访问认证"
                    description="密码、2FA 与访问安全"
                    color="#e6a23c"
                    :expanded="isSectionExpanded('section-auth')"
                    @update:expanded="toggleSection('section-auth', $event)"
                >
                  <template #icon>
                    <el-icon><Lock /></el-icon>
                  </template>
                  <template #header>
                    <div class="card-header">
                      <el-icon :size="20" color="#409eff">
                        <Lock />
                      </el-icon>
                      <span>Web 访问认证</span>
                    </div>
                  </template>

                  <AuthSettingsSection embedded />
                </CollapsibleSettingCard>

                <!-- 下载配置 -->
                <CollapsibleSettingCard
                    id="section-download"
                    title="下载配置"
                    description="目录、线程与重试策略"
                    color="#67c23a"
                    :expanded="isSectionExpanded('section-download')"
                    @update:expanded="toggleSection('section-download', $event)"
                >
                  <template #icon>
                    <el-icon><Download /></el-icon>
                  </template>
                  <template #header>
                    <div class="card-header">
                      <el-icon :size="20" color="#67c23a">
                        <Download />
                      </el-icon>
                      <span>下载配置</span>
                    </div>
                  </template>

                  <!-- VIP 等级信息 -->
                  <el-alert
                      v-if="recommended"
                      :title="`您的会员等级: ${recommended.vip_name}`"
                      type="info"
                      :closable="false"
                      style="margin-bottom: 20px"
                  >
                    <template #default>
                      <div class="vip-info">
                        <div class="vip-item">
                          <el-icon><User /></el-icon>
                          <span>推荐线程数: {{ recommended.recommended.threads }} 个</span>
                        </div>
                        <div class="vip-item">
                          <el-icon><Files /></el-icon>
                          <span>推荐分片大小: {{ recommended.recommended.chunk_size }} MB</span>
                        </div>
                        <div class="vip-item">
                          <el-icon><Download /></el-icon>
                          <span>最大同时下载: {{ recommended.recommended.max_tasks }} 个文件</span>
                        </div>
                      </div>
                    </template>
                  </el-alert>

                  <!-- 警告提示 -->
                  <el-alert
                      v-if="recommended && recommended.warnings && recommended.warnings.length > 0"
                      type="warning"
                      :closable="false"
                      style="margin-bottom: 20px"
                  >
                    <template #default>
                      <div v-for="(warning, index) in recommended.warnings" :key="index">
                        <div style="white-space: pre-wrap">{{ warning }}</div>
                      </div>
                    </template>
                  </el-alert>

                  <el-form-item label="下载目录" prop="download.download_dir">
                    <div class="input-with-button">
                      <el-input
                          v-model="formData.download.download_dir"
                          placeholder="请输入绝对路径，例如: /app/downloads 或 D:\Downloads"
                          clearable
                      >
                        <template #prepend>
                          <el-icon><Folder /></el-icon>
                        </template>
                      </el-input>
                      <el-button
                          type="primary"
                          @click="handleSelectDownloadDir"
                      >
                        <el-icon><FolderOpened /></el-icon>
                        <span v-if="!isMobile">浏览</span>
                      </el-button>
                    </div>
                    <div class="form-tip">
                      <div>文件下载的保存目录，必须使用绝对路径</div>
                      <div style="margin-top: 4px;">
                        Windows 示例: <code>D:\Downloads</code> 或 <code>C:\Example\Downloads</code><br/>
                        Linux/Docker 示例: <code>/app/downloads</code> 或 <code>/home/user/downloads</code>
                      </div>
                    </div>
                  </el-form-item>

                  <el-form-item label="下载时选择目录" prop="download.ask_each_time">
                    <el-switch
                        v-model="formData.download.ask_each_time"
                        active-text="每次询问"
                        inactive-text="使用默认"
                    />
                    <div class="form-tip">
                      开启后，每次下载都会弹出文件资源管理器让您选择保存位置；
                      关闭后将直接使用默认下载目录
                    </div>
                  </el-form-item>

                  <el-form-item label="全局最大线程数" prop="download.max_global_threads">
                    <el-slider
                        v-model="formData.download.max_global_threads"
                        :min="1"
                        :max="20"
                        :step="1"
                        :marks="threadMarks"
                        show-stops
                        style="width: calc(100% - 20px); margin-right: 20px"
                    />
                    <div class="value-display">
                      当前: {{ formData.download.max_global_threads }} 个
                      <span v-if="recommended" class="recommend-hint">
                    (推荐: {{ recommended.recommended.threads }} 个)
                  </span>
                    </div>
                    <div class="form-tip">
                      <el-icon><InfoFilled /></el-icon>
                      所有下载任务共享的线程池大小，单文件可使用全部线程进行分片下载
                    </div>
                    <div class="form-tip warning-tip" v-if="formData.download.max_global_threads > 10 && recommended && recommended.vip_type === 0">
                      ⚠️ 警告：普通用户建议保持1个线程，调大可能触发限速！
                    </div>
                  </el-form-item>

                  <el-form-item label="最大同时下载数" prop="download.max_concurrent_tasks">
                    <el-slider
                        v-model="formData.download.max_concurrent_tasks"
                        :min="1"
                        :max="10"
                        :step="1"
                        :marks="taskMarks"
                        show-stops
                        style="width: calc(100% - 20px); margin-right: 20px"
                    />
                    <div class="value-display">
                      当前: {{ formData.download.max_concurrent_tasks }} 个
                      <span v-if="recommended" class="recommend-hint">
                    (推荐: {{ recommended.recommended.max_tasks }} 个)
                  </span>
                    </div>
                    <div class="form-tip">
                      可以同时进行下载的文件数量上限
                    </div>
                  </el-form-item>

                  <!-- 分片大小说明（自适应，不可配置） -->
                  <el-alert
                      title="智能分片大小"
                      type="success"
                      :closable="false"
                      style="margin-bottom: 20px"
                  >
                    <template #default>
                      <div style="line-height: 1.8">
                        系统会根据文件大小和您的VIP等级自动选择最优分片大小：<br/>
                        • 小文件（<5MB）使用 256KB 分片<br/>
                        • 中等文件（5-10MB）使用 512KB 分片<br/>
                        • 中大型文件（10-500MB）使用 1MB-4MB 分片<br/>
                        • 大文件（≥500MB）使用 5MB 分片<br/>
                        • VIP限制：普通用户最高4MB，普通会员最高4MB，SVIP最高5MB<br/>
                        • 注意：百度网盘限制单个Range请求最大5MB，超过会返回403错误
                      </div>
                    </template>
                  </el-alert>

                  <el-form-item label="最大重试次数" prop="download.max_retries">
                    <el-input-number
                        v-model="formData.download.max_retries"
                        :min="0"
                        :max="10"
                        :step="1"
                        controls-position="right"
                        style="width: 100%"
                    />
                    <div class="form-tip">下载分片失败后的重试次数，0 表示不重试</div>
                  </el-form-item>
                </CollapsibleSettingCard>

                <!-- 上传配置 -->
                <CollapsibleSettingCard
                    id="section-upload"
                    title="上传配置"
                    description="上传并发、重试与过滤"
                    color="#e6a23c"
                    :expanded="isSectionExpanded('section-upload')"
                    @update:expanded="toggleSection('section-upload', $event)"
                >
                  <template #icon>
                    <el-icon><Upload /></el-icon>
                  </template>
                  <template #header>
                    <div class="card-header">
                      <el-icon :size="20" color="#e6a23c">
                        <Upload />
                      </el-icon>
                      <span>上传配置</span>
                    </div>
                  </template>

                  <el-form-item label="全局最大线程数" prop="upload.max_global_threads">
                    <el-slider
                        v-model="formData.upload.max_global_threads"
                        :min="1"
                        :max="20"
                        :step="1"
                        :marks="threadMarks"
                        show-stops
                        style="width: calc(100% - 20px); margin-right: 20px"
                    />
                    <div class="value-display">
                      当前: {{ formData.upload.max_global_threads }} 个
                    </div>
                    <div class="form-tip">
                      <el-icon><InfoFilled /></el-icon>
                      所有上传任务共享的线程池大小
                    </div>
                  </el-form-item>

                  <el-form-item label="最大同时上传数" prop="upload.max_concurrent_tasks">
                    <el-slider
                        v-model="formData.upload.max_concurrent_tasks"
                        :min="1"
                        :max="10"
                        :step="1"
                        :marks="taskMarks"
                        show-stops
                        style="width: calc(100% - 20px); margin-right: 20px"
                    />
                    <div class="value-display">
                      当前: {{ formData.upload.max_concurrent_tasks }} 个
                    </div>
                    <div class="form-tip">
                      可以同时进行上传的文件数量上限
                    </div>
                  </el-form-item>

                  <el-form-item label="最大重试次数" prop="upload.max_retries">
                    <el-input-number
                        v-model="formData.upload.max_retries"
                        :min="0"
                        :max="10"
                        :step="1"
                        controls-position="right"
                        style="width: 100%"
                    />
                    <div class="form-tip">上传分片失败后的重试次数，0 表示不重试</div>
                  </el-form-item>

                  <el-form-item label="跳过隐藏文件" prop="upload.skip_hidden_files">
                    <el-switch
                        v-model="formData.upload.skip_hidden_files"
                        active-text="跳过"
                        inactive-text="不跳过"
                    />
                    <div class="form-tip">
                      上传文件夹时是否跳过以"."开头的隐藏文件/文件夹（如 .git、.DS_Store 等）
                    </div>
                  </el-form-item>

                  <!-- 分片大小说明（自适应，不可配置） -->
                  <el-alert
                      title="智能分片大小（自动适配）"
                      type="success"
                      :closable="false"
                  >
                    <template #default>
                      <div style="line-height: 1.8">
                        系统会根据文件大小和您的VIP等级自动选择最优分片大小：<br/>
                        • 普通用户：固定 4MB 分片<br/>
                        • 普通会员：智能选择 4-16MB 分片<br/>
                        • 超级会员：智能选择 4-32MB 分片<br/>
                        <br/>
                        <strong>⚠️ 重要说明：</strong><br/>
                        • 上传时的实际分片大小（4-32MB）用于提升传输效率<br/>
                      </div>
                    </template>
                  </el-alert>
                </CollapsibleSettingCard>

                <!-- 冲突策略配置 -->
                <CollapsibleSettingCard
                    id="section-conflict"
                    title="冲突策略"
                    description="上传和下载重名处理"
                    color="#f56c6c"
                    :expanded="isSectionExpanded('section-conflict')"
                    @update:expanded="toggleSection('section-conflict', $event)"
                >
                  <template #icon>
                    <el-icon><Warning /></el-icon>
                  </template>
                  <template #header>
                    <div class="card-header">
                      <el-icon :size="20" color="#f56c6c">
                        <Warning />
                      </el-icon>
                      <span>冲突策略配置</span>
                    </div>
                  </template>

                  <el-alert
                      title="关于冲突策略"
                      type="info"
                      :closable="false"
                      style="margin-bottom: 20px"
                  >
                    <template #default>
                      <div style="line-height: 1.8">
                        当上传/下载的文件路径已存在时，系统将按照您选择的策略处理冲突。<br/>
                        这里配置的是默认策略，您也可以在每次操作时单独选择。
                      </div>
                    </template>
                  </el-alert>

                  <el-form-item label="上传默认策略">
                    <el-select
                        v-model="formData!.conflict_strategy!.default_upload_strategy"
                        placeholder="请选择上传冲突策略"
                        style="width: 100%"
                    >
                      <el-option value="smart_dedup" label="智能去重">
                        <div class="strategy-option">
                          <span>智能去重</span>
                          <el-tooltip content="比较文件内容，相同则秒传，不同则自动重命名" placement="right">
                            <el-icon class="info-icon"><InfoFilled /></el-icon>
                          </el-tooltip>
                        </div>
                      </el-option>
                      <el-option value="auto_rename" label="自动重命名">
                        <div class="strategy-option">
                          <span>自动重命名</span>
                          <el-tooltip content="如果远程路径已存在文件则自动生成新文件名" placement="right">
                            <el-icon class="info-icon"><InfoFilled /></el-icon>
                          </el-tooltip>
                        </div>
                      </el-option>
                      <el-option value="overwrite" label="覆盖">
                        <div class="strategy-option">
                          <span>覆盖</span>
                          <el-tooltip content="直接覆盖远程已存在的文件（危险操作）" placement="right">
                            <el-icon class="info-icon"><InfoFilled /></el-icon>
                          </el-tooltip>
                        </div>
                      </el-option>
                    </el-select>
                    <div class="form-tip">
                      上传文件时的默认冲突处理策略（推荐：智能去重）
                    </div>
                  </el-form-item>

                  <el-form-item label="下载默认策略">
                    <el-select
                        v-model="formData!.conflict_strategy!.default_download_strategy"
                        placeholder="请选择下载冲突策略"
                        style="width: 100%"
                    >
                      <el-option value="overwrite" label="覆盖">
                        <div class="strategy-option">
                          <span>覆盖</span>
                          <el-tooltip content="如果本地文件已存在则覆盖" placement="right">
                            <el-icon class="info-icon"><InfoFilled /></el-icon>
                          </el-tooltip>
                        </div>
                      </el-option>
                      <el-option value="skip" label="跳过">
                        <div class="strategy-option">
                          <span>跳过</span>
                          <el-tooltip content="如果本地文件已存在则跳过下载" placement="right">
                            <el-icon class="info-icon"><InfoFilled /></el-icon>
                          </el-tooltip>
                        </div>
                      </el-option>
                      <el-option value="auto_rename" label="自动重命名">
                        <div class="strategy-option">
                          <span>自动重命名</span>
                          <el-tooltip content="如果本地文件已存在则自动生成新文件名" placement="right">
                            <el-icon class="info-icon"><InfoFilled /></el-icon>
                          </el-tooltip>
                        </div>
                      </el-option>
                    </el-select>
                    <div class="form-tip">
                      下载文件时的默认冲突处理策略（推荐：覆盖）
                    </div>
                  </el-form-item>
                </CollapsibleSettingCard>

                <!-- 转存配置 -->
                <CollapsibleSettingCard
                    id="section-transfer"
                    title="转存配置"
                    description="分享转存与剪贴板识别"
                    color="#909399"
                    :expanded="isSectionExpanded('section-transfer')"
                    @update:expanded="toggleSection('section-transfer', $event)"
                >
                  <template #icon>
                    <el-icon><Share /></el-icon>
                  </template>
                  <template #header>
                    <div class="card-header">
                      <el-icon :size="20" color="#909399">
                        <Share />
                      </el-icon>
                      <span>转存配置</span>
                    </div>
                  </template>

                  <el-form-item label="默认行为">
                    <el-radio-group v-model="transferBehavior">
                      <el-radio value="transfer_only">仅转存到网盘</el-radio>
                      <el-radio value="transfer_and_download">转存后自动下载</el-radio>
                    </el-radio-group>
                    <div class="form-tip">
                      选择"转存后自动下载"时，会根据下载配置决定是否弹出文件选择器
                    </div>
                  </el-form-item>
                  <el-form-item label="剪贴板识别">
                    <el-switch
                      v-if="formData?.mobile"
                      v-model="formData.mobile.clipboard_share_detection_enabled"
                    />
                    <div class="form-tip">
                      开启后，安卓 App 启动或回到前台时会在本地识别剪贴板中的百度网盘分享链接，并在顶部胶囊提示。
                    </div>
                  </el-form-item>
                </CollapsibleSettingCard>

                <!-- 加密设置 -->
                <CollapsibleSettingCard
                    id="section-encryption"
                    title="加密设置"
                    description="密钥、导出与解密数据"
                    color="#f56c6c"
                    :expanded="isSectionExpanded('section-encryption')"
                    @update:expanded="toggleSection('section-encryption', $event)"
                >
                  <template #icon>
                    <el-icon><Key /></el-icon>
                  </template>
                  <template #header>
                    <div class="card-header">
                      <el-icon :size="20" color="#f56c6c">
                        <Key />
                      </el-icon>
                      <span>加密设置</span>
                    </div>
                  </template>
                  <!-- 免责声明 -->
                  <el-alert
                      type="error"
                      :closable="false"
                      style="margin-bottom: 20px; border-left: 4px solid #f56c6c;"
                  >
                    <template #title>
                      <div style="font-weight: 600; margin-bottom: 8px;">⚠️ 重要提示</div>
                      <div style="line-height: 1.8; font-size: 13px;">
                        本加密功能采用客户端侧加密技术，可在一定程度上保护您的文件隐私，但请注意：
                        <ul style="margin: 8px 0 0 0; padding-left: 20px; line-height: 1.8;">
                          <li>加密技术无法提供100%的绝对安全保障，百度官方可能通过其他技术手段检测到加密文件</li>
                          <li>强烈建议对重要文件进行多重备份，并妥善保管加密密钥</li>
                          <li>使用前请充分了解相关风险，并自行评估是否适合您的使用场景</li>
                        </ul>
                      </div>
                    </template>
                  </el-alert>

                  <!-- 加密状态卡片 -->
                  <div class="encryption-status-card">
                    <div class="status-header">
                      <span class="status-label">加密密钥状态</span>
                      <el-tag :type="encryptionStatus?.has_key ? 'success' : 'info'" size="small">
                        {{ encryptionStatus?.has_key ? '已配置' : '未配置' }}
                      </el-tag>
                    </div>
                    <div v-if="encryptionStatus?.has_key" class="status-detail">
                      算法: {{ encryptionStatus.algorithm }}<br>
                      创建时间: {{ encryptionStatus.key_created_at ? formatDate(encryptionStatus.key_created_at) : '-' }}
                    </div>
                  </div>

                  <!-- 未配置密钥时显示 -->
                  <div v-if="!encryptionStatus?.has_key" class="encryption-form">
                    <el-form-item label="加密算法">
                      <el-select v-model="keyAlgorithm" style="width: 100%">
                        <el-option value="AES-256-GCM" label="AES-256-GCM（推荐）" />
                        <el-option value="ChaCha20-Poly1305" label="ChaCha20-Poly1305" />
                      </el-select>
                    </el-form-item>
                    <el-button type="primary" style="width: 100%" @click="handleGenerateKey">
                      <el-icon><Key /></el-icon>
                      生成新密钥
                    </el-button>
                    <el-divider>或</el-divider>
                    <el-form-item label="导入密钥">
                      <el-input v-model="encryptionKey" placeholder="粘贴Base64编码的密钥" />
                    </el-form-item>
                    <el-button style="width: 100%" @click="handleImportKey">
                      <el-icon><Upload /></el-icon>
                      导入密钥
                    </el-button>
                  </div>

                  <!-- 已配置密钥时显示 -->
                  <div v-else class="encryption-actions">
                    <el-button @click="handleExportKey">
                      <el-icon><CopyDocument /></el-icon>
                      导出密钥
                    </el-button>
                    <el-button type="danger" plain @click="handleDeleteKey">
                      <el-icon><Delete /></el-icon>
                      删除密钥
                    </el-button>
                  </div>

                  <!-- 导出解密数据区域 -->
                  <el-divider v-if="encryptionStatus?.has_key" content-position="left">导出解密数据</el-divider>
                  <div v-if="encryptionStatus?.has_key" class="export-decrypt-section">
                    <el-alert
                        type="warning"
                        :closable="false"
                        show-icon
                        style="margin-bottom: 16px"
                    >
                      <template #title>
                        <strong>安全提示：</strong>导出的数据包含敏感的加密密钥和文件映射信息，请妥善保管！
                      </template>
                    </el-alert>

                    <div class="export-actions">
                      <el-button type="primary" @click="handleExportDecryptBundle" :loading="exportingBundle">
                        <el-icon><Download /></el-icon>
                        导出解密数据包
                      </el-button>
                      <el-dropdown @command="handleSeparateExport" trigger="click">
                        <el-button :loading="exportingSeparate">
                          <el-icon><Document /></el-icon>
                          分别导出
                          <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                        </el-button>
                        <template #dropdown>
                          <el-dropdown-menu>
                            <el-dropdown-item command="keys">导出密钥配置 (encryption.json)</el-dropdown-item>
                            <el-dropdown-item command="mapping">导出映射数据 (mapping.json)</el-dropdown-item>
                          </el-dropdown-menu>
                        </template>
                      </el-dropdown>
                    </div>

                    <div class="form-tip" style="margin-top: 12px">
                      <strong>解密数据包</strong>：包含 encryption.json（密钥配置）和 mapping.json（文件映射），可配合 decrypt-cli 工具在其他机器上解密文件。
                    </div>
                  </div>

                  <el-alert type="warning" :closable="false" show-icon style="margin-top: 16px">
                    <template #title>
                      <strong>重要提示：</strong>请妥善保管加密密钥。如果密钥丢失，将无法解密已加密的文件！
                    </template>
                  </el-alert>

                  <div class="form-tip" style="margin-top: 12px">
                    加密密钥用于上传等本地安全能力。请妥善保存，密钥丢失后将无法解密已加密的文件。
                  </div>
                </CollapsibleSettingCard>

                <!-- 网络代理配置 -->
                <CollapsibleSettingCard
                    id="section-proxy"
                    title="网络代理"
                    description="HTTP/SOCKS5 与连接状态"
                    color="#9b59b6"
                    :expanded="isSectionExpanded('section-proxy')"
                    @update:expanded="toggleSection('section-proxy', $event)"
                >
                  <template #icon>
                    <el-icon><Promotion /></el-icon>
                  </template>
                  <template #header>
                    <div class="card-header">
                      <el-icon :size="20" color="#9b59b6">
                        <Promotion />
                      </el-icon>
                      <span>网络代理</span>
                    </div>
                  </template>

                  <el-form-item label="代理类型">
                    <el-radio-group v-model="proxyType" @change="handleProxyTypeChange">
                      <el-radio value="none">无代理</el-radio>
                      <el-radio value="http">HTTP 代理</el-radio>
                      <el-radio value="socks5">SOCKS5 代理</el-radio>
                    </el-radio-group>
                  </el-form-item>

                  <template v-if="proxyType !== 'none'">
                    <el-form-item label="主机地址">
                      <el-input
                          v-model="proxyHost"
                          placeholder="例如: 127.0.0.1"
                          clearable
                      />
                    </el-form-item>

                    <el-form-item label="端口">
                      <el-input-number
                          v-model="proxyPort"
                          :min="1"
                          :max="65535"
                          :step="1"
                          controls-position="right"
                          style="width: 100%"
                      />
                    </el-form-item>

                    <el-form-item label="用户名">
                      <el-input
                          v-model="proxyUsername"
                          placeholder="可选"
                          clearable
                      />
                    </el-form-item>

                    <el-form-item label="密码">
                      <el-input
                          v-model="proxyPassword"
                          type="password"
                          placeholder="可选"
                          show-password
                          clearable
                          autocomplete="new-password"
                      />
                    </el-form-item>

                    <el-form-item label="自动回退">
                      <el-switch
                          v-model="proxyAllowFallback"
                          active-text="允许"
                          inactive-text="禁止"
                      />
                      <div class="form-tip" style="margin-top: 4px;">
                        代理故障时自动回退到直连模式。关闭后，若代理不可用将无法访问本页面，需手动编辑 config/app.toml 修改代理配置。
                      </div>
                    </el-form-item>

                    <!-- 代理运行状态指示器 -->
                    <el-form-item label="运行状态">
                      <div class="proxy-status-indicator">
                        <el-tag
                            v-if="proxyRuntimeStatus === 'normal'"
                            type="success"
                            effect="light"
                            size="small"
                        >
                          ● 代理正常
                        </el-tag>
                        <el-tag
                            v-else-if="proxyRuntimeStatus === 'fallen_back_to_direct'"
                            type="warning"
                            effect="light"
                            size="small"
                        >
                          ● 已回退到直连
                        </el-tag>
                        <el-tag
                            v-else-if="proxyRuntimeStatus === 'probing'"
                            type="primary"
                            effect="light"
                            size="small"
                            class="probing-tag"
                        >
                          ● 探测中
                          <span v-if="proxyNextProbeIn !== null"> ({{ proxyNextProbeIn }}s)</span>
                        </el-tag>
                        <el-tag
                            v-else
                            type="info"
                            effect="light"
                            size="small"
                        >
                          ● 未配置
                        </el-tag>
                        <span v-if="proxyFlapCount > 0" class="flap-count">
                      抖动次数: {{ proxyFlapCount }}
                    </span>
                      </div>
                    </el-form-item>

                    <!-- 测试连接按钮 -->
                    <el-form-item label=" ">
                      <el-button
                          type="primary"
                          plain
                          :loading="testingProxy"
                          @click="handleTestProxy"
                          :disabled="!proxyHost || !proxyPort"
                      >
                        <el-icon v-if="!testingProxy"><Connection /></el-icon>
                        测试连接
                      </el-button>
                    </el-form-item>
                  </template>

                  <div class="form-tip">
                    配置代理后，所有网络请求（登录、API 调用、文件下载）将通过代理服务器转发。保存后立即生效，无需重启。
                  </div>
                </CollapsibleSettingCard>

                <!-- 关于信息 -->
                <CollapsibleSettingCard
                    id="section-about"
                    title="关于"
                    description="版本、来源与开源许可"
                    color="#909399"
                    :expanded="isSectionExpanded('section-about')"
                    @update:expanded="toggleSection('section-about', $event)"
                >
                  <template #icon>
                    <el-icon><InfoFilled /></el-icon>
                  </template>
                  <template #header>
                    <div class="card-header">
                      <el-icon :size="20" color="#909399">
                        <InfoFilled />
                      </el-icon>
                      <span>关于</span>
                    </div>
                  </template>

                  <div class="about-content">
                    <div class="about-item">
                      <span class="label">项目名称:</span>
                      <span class="value">{{ APP_DISPLAY_NAME }}</span>
                    </div>
                    <div class="about-item">
                      <span class="label">版本:</span>
                      <span class="value">v{{ appVersion }}</span>
                    </div>
                    <div class="about-item">
                      <span class="label">后端技术:</span>
                      <span class="value">Rust + Axum + Tokio</span>
                    </div>
                    <div class="about-item">
                      <span class="label">前端技术:</span>
                      <span class="value">Vue 3 + TypeScript + Element Plus</span>
                    </div>
                    <div class="about-item">
                      <span class="label">上游项目:</span>
                      <span class="value">{{ UPSTREAM_PROJECT_NAME }} {{ UPSTREAM_VERSION }}</span>
                    </div>
                    <div class="about-item">
                      <span class="label">许可证:</span>
                      <span class="value">Apache License 2.0</span>
                    </div>
                    <div class="about-item">
                      <span class="label">开源说明:</span>
                      <span class="value">本应用基于开源项目移植，并已补齐许可证、版权与第三方依赖清单。</span>
                    </div>
                    <div class="about-item source-item">
                      <span class="label">许可证、版权与鸣谢:</span>
                      <router-link class="value value-link" to="/about/credits">
                        查看开源许可与鸣谢
                      </router-link>
                    </div>
                  </div>
                </CollapsibleSettingCard>
              </el-form>
            </el-skeleton>
          </div>
        </div>
      </el-main>
    </el-container>

    <!-- 目录选择器 -->
    <FilePickerModal
        v-model="showDirPicker"
        mode="select-directory"
        title="选择下载目录"
        confirm-text="确定"
        :initial-path="formData?.download?.download_dir"
        @confirm="handleDirConfirm"
    />

    <!-- 密钥显示对话框 -->
    <el-dialog v-model="showKeyDialog" title="加密密钥" width="450px" :close-on-click-modal="false">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px">
        <template #title>请立即备份此密钥到安全的地方！</template>
      </el-alert>
      <el-input
          :model-value="encryptionKey"
          :type="showKey ? 'text' : 'password'"
          readonly
          class="key-input"
      >
        <template #suffix>
          <el-button link @click="showKey = !showKey">
            <el-icon v-if="!showKey"><View /></el-icon>
            <el-icon v-else><Hide /></el-icon>
          </el-button>
          <el-button link @click="copyToClipboard(encryptionKey)">
            <el-icon><CopyDocument /></el-icon>
          </el-button>
        </template>
      </el-input>
      <template #footer>
        <el-button type="primary" style="width: 100%" @click="showKeyDialog = false; encryptionKey = ''; showKey = false">
          我已保存密钥
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useIsMobile } from '@/utils/responsive'
import { usePageVisibility } from '@/utils/pageVisibility'
import { useConfigStore } from '@/stores/config'
import {
  APP_DISPLAY_NAME,
  UPSTREAM_PROJECT_NAME,
  UPSTREAM_VERSION,
  getAppVersion,
} from '@/constants/appInfo'
import type { AppConfig, ProxyType, ProxyRuntimeStatus } from '@/api/config'
import { getRecommendedConfig, resetToRecommended, getProxyStatus, testProxyConnection } from '@/api/config'
import type { UploadConflictStrategy } from '@/api/upload'
import type { DownloadConflictStrategy } from '@/api/download'
import { FilePickerModal } from '@/components/FilePicker'
import AuthSettingsSection from '@/components/settings/AuthSettingsSection.vue'
import CollapsibleSettingCard from '@/components/settings/CollapsibleSettingCard.vue'
import {
  Check,
  RefreshLeft,
  Monitor,
  Lock,
  Connection,
  Warning,
  Download,
  Upload,
  Folder,
  InfoFilled,
  User,
  Files,
  Share,
  FolderOpened,
  Key,
  CopyDocument,
  Delete,
  View,
  Hide,
  Document,
  ArrowDown,
  Promotion,
} from '@element-plus/icons-vue'
import { getTransferConfig, updateTransferConfig } from '@/api/config'
import {
  getEncryptionStatus,
  generateEncryptionKey,
  importEncryptionKey,
  exportEncryptionKey,
  deleteEncryptionKey,
  exportDecryptBundle,
  downloadMappingJson,
  downloadKeysJson,
  type EncryptionStatus,
} from '@/api/autobackup'

const configStore = useConfigStore()

// 响应式检测
const isMobile = useIsMobile()
const isPageVisible = usePageVisibility()
const appVersion = getAppVersion()

// 状态
const loading = ref(false)
const saving = ref(false)
const resetting = ref(false)
const formRef = ref<FormInstance>()
const formData = ref<AppConfig | null>(null)
const recommended = ref<any>(null)
const transferBehavior = ref('transfer_only')
const showDirPicker = ref(false)

// 锚点导航
const contentRef = ref<HTMLElement>()
const activeSection = ref('section-server')
let sectionObserver: IntersectionObserver | null = null

const navItems = [
  { id: 'section-server', label: '服务器', color: '#409eff' },
  { id: 'section-auth', label: '访问认证', color: '#e6a23c' },
  { id: 'section-download', label: '下载', color: '#67c23a' },
  { id: 'section-upload', label: '上传', color: '#e6a23c' },
  { id: 'section-conflict', label: '冲突策略', color: '#f56c6c' },
  { id: 'section-transfer', label: '转存', color: '#909399' },
  { id: 'section-encryption', label: '加密', color: '#f56c6c' },
  { id: 'section-proxy', label: '网络代理', color: '#9b59b6' },
  { id: 'section-about', label: '关于', color: '#909399' },
]

const expandedSections = ref<string[]>([])

const settingPropSectionMap: Record<string, string> = {
  server: 'section-server',
  download: 'section-download',
  upload: 'section-upload',
  conflict_strategy: 'section-conflict',
  mobile: 'section-transfer',
  network: 'section-proxy',
}

function isSectionExpanded(id: string) {
  return !isMobile.value || expandedSections.value.includes(id)
}

function toggleSection(id: string, expanded: boolean) {
  if (!isMobile.value) return
  const current = new Set(expandedSections.value)
  if (expanded) {
    current.add(id)
  } else {
    current.delete(id)
  }
  expandedSections.value = Array.from(current)
}

function expandSection(id: string) {
  toggleSection(id, true)
}

function sectionIdFromProp(prop?: string) {
  if (!prop) return ''
  const root = prop.split('.')[0]
  return settingPropSectionMap[root] || ''
}

async function expandSectionForValidationError(error: any) {
  if (!isMobile.value || !error) return
  const firstProp = Object.keys(error)[0]
  const sectionId = sectionIdFromProp(firstProp)
  if (!sectionId) return
  expandSection(sectionId)
  await nextTick()
  document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// 代理相关状态
const proxyType = ref<ProxyType>('none')
const proxyHost = ref('')
const proxyPort = ref(0)
const proxyUsername = ref('')
const proxyPassword = ref('')
const proxyAllowFallback = ref(true)

// 代理测试连接
const testingProxy = ref(false)

// 代理运行状态
const proxyRuntimeStatus = ref<ProxyRuntimeStatus>('no_proxy')
const proxyFlapCount = ref(0)
const proxyNextProbeIn = ref<number | null>(null)
let proxyStatusTimer: ReturnType<typeof setInterval> | null = null
const PROXY_STATUS_POLL_MS = 15_000

// 加密相关状态
const encryptionStatus = ref<EncryptionStatus | null>(null)
const encryptionKey = ref('')
const keyAlgorithm = ref('AES-256-GCM')
const showKeyDialog = ref(false)
const showKey = ref(false)

// 导出解密数据相关状态
const exportingBundle = ref(false)
const exportingSeparate = ref(false)

// 滑块标记
const threadMarks = reactive({
  1: '1',
  5: '5',
  10: '10',
  15: '15',
  20: '20',
})

const taskMarks = reactive({
  1: '1',
  3: '3',
  5: '5',
  7: '7',
  10: '10',
})

// 表单验证规则
const rules = reactive<FormRules<AppConfig>>({
  'server.host': [
    { required: true, message: '请输入监听地址', trigger: 'blur' },
  ],
  'server.port': [
    { required: true, message: '请输入监听端口', trigger: 'blur' },
    { type: 'number', min: 1, max: 65535, message: '端口范围: 1-65535', trigger: 'blur' },
  ],
  'download.download_dir': [
    { required: true, message: '请输入下载目录', trigger: 'blur' },
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (!value) {
          callback(new Error('请输入下载目录'))
          return
        }
        // 检查是否为绝对路径
        // Windows: 以盘符开头 (如 C:\, D:\) 或 UNC路径 (\\server\share)
        // Linux/Mac: 以 / 开头
        const isWindowsAbsolute = /^[A-Za-z]:\\/.test(value) || /^\\\\/.test(value)
        const isUnixAbsolute = /^\//.test(value)

        if (!isWindowsAbsolute && !isUnixAbsolute) {
          callback(new Error('请输入绝对路径（Windows: D:\\Downloads, Linux: /app/downloads）'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  'download.max_global_threads': [
    { required: true, message: '请选择全局最大线程数', trigger: 'change' },
    { type: 'number', min: 1, max: 20, message: '线程数范围: 1-20', trigger: 'change' },
  ],
  'download.max_concurrent_tasks': [
    { required: true, message: '请选择最大同时下载数', trigger: 'change' },
    { type: 'number', min: 1, max: 10, message: '同时下载数范围: 1-10', trigger: 'change' },
  ],
  'download.max_retries': [
    { required: true, message: '请输入最大重试次数', trigger: 'blur' },
    { type: 'number', min: 0, max: 10, message: '重试次数范围: 0-10', trigger: 'blur' },
  ],
  'upload.max_global_threads': [
    { required: true, message: '请选择上传全局最大线程数', trigger: 'change' },
    { type: 'number', min: 1, max: 20, message: '线程数范围: 1-20', trigger: 'change' },
  ],
  'upload.max_concurrent_tasks': [
    { required: true, message: '请选择最大同时上传数', trigger: 'change' },
    { type: 'number', min: 1, max: 10, message: '同时上传数范围: 1-10', trigger: 'change' },
  ],
  'upload.max_retries': [
    { required: true, message: '请输入最大重试次数', trigger: 'blur' },
    { type: 'number', min: 0, max: 10, message: '重试次数范围: 0-10', trigger: 'blur' },
  ],
})

// 加载配置
async function loadConfig() {
  loading.value = true
  try {
    const config = await configStore.fetchConfig()
    formData.value = JSON.parse(JSON.stringify(config)) // 深拷贝

    // 初始化冲突策略配置（如果不存在则使用默认值）
    if (formData.value && !formData.value.conflict_strategy) {
      formData.value.conflict_strategy = {
        default_upload_strategy: 'smart_dedup' as UploadConflictStrategy,
        default_download_strategy: 'overwrite' as DownloadConflictStrategy
      }
    }

    // 初始化移动端体验配置
    if (formData.value && !formData.value.mobile) {
      formData.value.mobile = {
        clipboard_share_detection_enabled: true,
      }
    }

    // 初始化代理配置状态
    if (formData.value?.network?.proxy) {
      const p = formData.value.network.proxy
      proxyType.value = p.proxy_type || 'none'
      proxyHost.value = p.host || ''
      proxyPort.value = p.port || 0
      proxyUsername.value = p.username || ''
      proxyPassword.value = p.password || ''
      proxyAllowFallback.value = p.allow_fallback !== false
    } else {
      proxyType.value = 'none'
      proxyHost.value = ''
      proxyPort.value = 0
      proxyUsername.value = ''
      proxyPassword.value = ''
      proxyAllowFallback.value = true
    }

    // 同时加载推荐配置
    try {
      recommended.value = await getRecommendedConfig()
    } catch (error) {
      console.warn('获取推荐配置失败:', error)
    }

    // 加载转存配置
    try {
      const transferConfig = await getTransferConfig()
      transferBehavior.value = transferConfig.default_behavior || 'transfer_only'
    } catch (error) {
      console.warn('获取转存配置失败:', error)
    }

    syncProxyStatusPolling()
  } catch (error: any) {
    ElMessage.error('加载配置失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
    nextTick(() => initSectionObserver())
  }
}

// 恢复推荐配置
async function handleReset() {
  try {
    await ElMessageBox.confirm(
        '确定要恢复为推荐配置吗？这将根据您的VIP等级应用最佳配置。',
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        }
    )

    resetting.value = true
    await resetToRecommended()
    ElMessage.success('已恢复为推荐配置')

    // 重新加载配置
    await loadConfig()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('恢复配置失败: ' + (error.message || '未知错误'))
    }
  } finally {
    resetting.value = false
  }
}

// 保存配置
async function handleSave() {
  if (!formRef.value || !formData.value) return

  try {
    // 验证表单
    await formRef.value.validate().catch(async (fields) => {
      await expandSectionForValidationError(fields)
      throw fields
    })

    // 代理启用时验证必填字段
    if (proxyType.value !== 'none') {
      if (!proxyHost.value || !proxyHost.value.trim()) {
        expandSection('section-proxy')
        ElMessage.error('代理主机地址不能为空')
        return
      }
      if (!proxyPort.value || proxyPort.value <= 0) {
        expandSection('section-proxy')
        ElMessage.error('代理端口不能为空')
        return
      }
    }

    saving.value = true
    // 同步代理配置到 formData
    if (!formData.value.network) {
      formData.value.network = { proxy: { proxy_type: 'none', host: '', port: 0 } }
    }
    formData.value.network.proxy = {
      proxy_type: proxyType.value,
      host: proxyHost.value,
      port: proxyPort.value,
      username: proxyUsername.value || undefined,
      password: proxyPassword.value || undefined,
      allow_fallback: proxyAllowFallback.value,
    }
    await configStore.saveConfig(formData.value)

    // 同时保存转存配置
    try {
      await updateTransferConfig({ default_behavior: transferBehavior.value })
    } catch (error) {
      console.warn('保存转存配置失败:', error)
    }

    ElMessage.success(proxyType.value !== 'none' ? '代理配置已保存并生效' : '配置已保存')

    syncProxyStatusPolling()

    // 重新加载推荐配置以更新警告
    try {
      recommended.value = await getRecommendedConfig()
    } catch (error) {
      console.warn('更新推荐配置失败:', error)
    }
  } catch (error: any) {
    if (error && typeof error === 'object' && !error.response && Object.keys(error).length > 0) {
      ElMessage.error('请检查已展开的设置项')
      return
    }

    // 提取详细的错误消息
    let errorMessage = '未知错误'

    if (error.response?.data?.details) {
      // 后端返回的详细错误信息在 response.data.details 中
      errorMessage = error.response.data.details
    } else if (error.response?.data?.message) {
      // 后端返回的通用错误消息
      errorMessage = error.response.data.message
    } else if (error.message) {
      // axios 默认的错误消息
      errorMessage = error.message
    }

    ElMessage.error('保存配置失败: ' + errorMessage)
  } finally {
    saving.value = false
  }
}

// 选择下载目录
function handleSelectDownloadDir() {
  showDirPicker.value = true
}

// 目录选择确认
function handleDirConfirm(path: string) {
  if (formData.value && path) {
    formData.value.download.download_dir = path
    ElMessage.success('已选择目录: ' + path)
  }
  showDirPicker.value = false
}

// 代理类型变更
function handleProxyTypeChange(val: ProxyType) {
  if (val === 'none') {
    proxyHost.value = ''
    proxyPort.value = 0
    proxyUsername.value = ''
    proxyPassword.value = ''
  }
}

// 测试代理连接
async function handleTestProxy() {
  if (!proxyHost.value || !proxyPort.value) {
    ElMessage.warning('请先填写代理主机和端口')
    return
  }
  testingProxy.value = true
  try {
    const resp = await testProxyConnection({
      proxy_type: proxyType.value,
      host: proxyHost.value,
      port: proxyPort.value,
      username: proxyUsername.value || undefined,
      password: proxyPassword.value || undefined,
      allow_fallback: proxyAllowFallback.value,
    })
    if (resp.success) {
      ElMessage.success(`代理连接成功，延迟 ${resp.latency_ms}ms`)
    } else {
      ElMessage.error(`代理连接失败: ${resp.error}`)
    }
  } catch (e: any) {
    ElMessage.error(e.message || '测试请求失败')
  } finally {
    testingProxy.value = false
  }
}

// 获取代理运行状态
async function fetchProxyStatus() {
  if (!isPageVisible.value || proxyType.value === 'none') {
    return
  }

  try {
    const resp = await getProxyStatus()
    proxyRuntimeStatus.value = resp.status
    proxyFlapCount.value = resp.flap_count
    proxyNextProbeIn.value = resp.next_probe_in_secs
  } catch {
    // 静默失败，不影响用户操作
  }
}

function syncProxyStatusPolling() {
  if (proxyType.value !== 'none' && isPageVisible.value) {
    startProxyStatusPolling()
    return
  }

  stopProxyStatusPolling()
}

// 启动代理状态轮询（15秒间隔，仅在页面可见时运行）
function startProxyStatusPolling() {
  if (proxyType.value === 'none' || !isPageVisible.value) {
    stopProxyStatusPolling()
    return
  }

  stopProxyStatusPolling()
  fetchProxyStatus()
  proxyStatusTimer = setInterval(() => {
    if (!isPageVisible.value || proxyType.value === 'none') {
      stopProxyStatusPolling()
      return
    }

    fetchProxyStatus()
  }, PROXY_STATUS_POLL_MS)
}

// 停止代理状态轮询
function stopProxyStatusPolling() {
  if (proxyStatusTimer) {
    clearInterval(proxyStatusTimer)
    proxyStatusTimer = null
  }
}

// 加载加密状态
async function loadEncryptionStatus() {
  try {
    encryptionStatus.value = await getEncryptionStatus()
  } catch (error) {
    console.warn('获取加密状态失败:', error)
  }
}

// 生成密钥
async function handleGenerateKey() {
  try {
    const key = await generateEncryptionKey(keyAlgorithm.value)
    encryptionKey.value = key
    showKeyDialog.value = true
    await loadEncryptionStatus()
    ElMessage.success('密钥生成成功，请妥善保管')
  } catch (error: any) {
    ElMessage.error('生成密钥失败: ' + (error.message || '未知错误'))
  }
}

// 导入密钥
async function handleImportKey() {
  if (!encryptionKey.value) {
    ElMessage.warning('请输入密钥')
    return
  }
  try {
    await importEncryptionKey(encryptionKey.value, keyAlgorithm.value)
    encryptionKey.value = ''
    await loadEncryptionStatus()
    ElMessage.success('密钥导入成功')
  } catch (error: any) {
    ElMessage.error('导入密钥失败: ' + (error.message || '未知错误'))
  }
}

// 导出密钥
async function handleExportKey() {
  try {
    const key = await exportEncryptionKey()
    encryptionKey.value = key
    showKeyDialog.value = true
  } catch (error: any) {
    ElMessage.error('导出密钥失败: ' + (error.message || '未知错误'))
  }
}

// 删除密钥
async function handleDeleteKey() {
  try {
    await ElMessageBox.confirm(
        '确定要删除加密密钥吗？删除后将无法解密已加密的文件！',
        '危险操作',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'error',
        }
    )
    await deleteEncryptionKey()
    await loadEncryptionStatus()
    ElMessage.success('密钥已删除')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除密钥失败: ' + (error.message || '未知错误'))
    }
  }
}

// 导出解密数据包
async function handleExportDecryptBundle() {
  try {
    // 显示风险提示对话框
    await ElMessageBox.confirm(
        '导出的数据包包含敏感的加密密钥和文件映射信息。\n\n请注意：\n• 妥善保管导出的文件，避免泄露\n• 不要将文件上传到公共网络或分享给他人\n• 建议将文件存储在安全的离线位置',
        '安全提示',
        {
          confirmButtonText: '我已了解，继续导出',
          cancelButtonText: '取消',
          type: 'warning',
        }
    )

    exportingBundle.value = true
    await exportDecryptBundle()
    ElMessage.success('解密数据包导出成功')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('导出失败: ' + (error.message || '未知错误'))
    }
  } finally {
    exportingBundle.value = false
  }
}

// 分别导出密钥或映射
async function handleSeparateExport(command: string) {
  try {
    // 显示风险提示对话框
    await ElMessageBox.confirm(
        command === 'keys'
            ? '导出的密钥文件包含敏感的加密密钥信息。\n\n请注意：\n• 妥善保管导出的文件，避免泄露\n• 密钥丢失将无法解密已加密的文件'
            : '导出的映射文件包含文件名和路径的对应关系。\n\n请注意：\n• 映射文件需要配合密钥文件使用\n• 妥善保管，避免泄露文件结构信息',
        '安全提示',
        {
          confirmButtonText: '我已了解，继续导出',
          cancelButtonText: '取消',
          type: 'warning',
        }
    )

    exportingSeparate.value = true
    if (command === 'keys') {
      await downloadKeysJson()
      ElMessage.success('密钥配置导出成功')
    } else if (command === 'mapping') {
      await downloadMappingJson()
      ElMessage.success('映射数据导出成功')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('导出失败: ' + (error.message || '未知错误'))
    }
  } finally {
    exportingSeparate.value = false
  }
}

// 复制到剪贴板
function copyToClipboard(text: string) {
  navigator.clipboard.writeText(text)
  ElMessage.success('已复制到剪贴板')
}

// 格式化日期
function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleString('zh-CN')
}

// 组件挂载
// 锚点导航：平滑滚动到指定分区
function scrollToSection(id: string) {
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

// 锚点导航：初始化 IntersectionObserver 追踪当前可见分区
function initSectionObserver() {
  if (isMobile.value) return
  const root = contentRef.value
  if (!root) return
  const ids = navItems.map(n => n.id)
  sectionObserver = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            activeSection.value = entry.target.id
          }
        }
      },
      { root, rootMargin: '-10% 0px -60% 0px', threshold: 0 }
  )
  for (const id of ids) {
    const el = document.getElementById(id)
    if (el) sectionObserver.observe(el)
  }
}

watch([isPageVisible, proxyType], () => {
  syncProxyStatusPolling()
})

onMounted(() => {
  loadConfig()
  loadEncryptionStatus()
  // 代理状态轮询在 loadConfig 完成后根据代理类型决定是否启动
})

// 组件卸载
onUnmounted(() => {
  stopProxyStatusPolling()
  if (sectionObserver) {
    sectionObserver.disconnect()
    sectionObserver = null
  }
})
</script>

<style scoped lang="scss">
.settings-container {
  width: 100%;
  height: 100vh;
  background: var(--app-bg);
}

.el-container {
  height: 100%;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--app-surface);
  border-bottom: 1px solid var(--app-border);
  padding: 0 20px;

  h2 {
    margin: 0;
    font-size: 20px;
    color: var(--app-text);
  }

  .header-actions {
    display: flex;
    gap: 10px;
  }
}

.el-main {
  padding: 0;
  overflow: hidden;
}

.settings-layout {
  display: flex;
  height: 100%;
}

.settings-nav {
  width: 120px;
  min-width: 120px;
  background: var(--app-surface);
  border-right: 1px solid var(--app-border);
  padding: 16px 0;
  overflow-y: auto;
  position: sticky;
  top: 0;

  ul {
    list-style: none;
    margin: 0;
    padding: 0;
  }

  li {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 16px;
    cursor: pointer;
    font-size: 13px;
    color: var(--app-text-secondary);
    transition: all 0.2s;
    border-left: 3px solid transparent;

    &:hover {
      color: var(--app-text);
      background: var(--app-surface-muted);
    }

    &.active {
      color: #409eff;
      font-weight: 600;
      background: var(--app-accent-soft);
      border-left-color: #409eff;
    }
  }

  .nav-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  .nav-label {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.settings-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  scroll-behavior: smooth;
}

.setting-card {
  margin-bottom: 20px;

  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    color: var(--app-text);
  }
}

.form-tip {
  margin-top: 4px;
  font-size: 12px;
  color: var(--app-text-secondary);
  line-height: 1.5;
}

.value-display {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
  text-align: right;

  .recommend-hint {
    font-size: 12px;
    font-weight: normal;
    color: #67c23a;
    margin-left: 8px;
  }
}

.warning-tip {
  color: #e6a23c !important;
  font-weight: 600;
  margin-top: 8px;
}

.input-with-button {
  display: flex;
  gap: 10px;
  width: 100%;

  .el-input {
    flex: 1;
  }
}

.vip-info {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  margin-top: 10px;

  .vip-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: var(--app-text-secondary);

    .el-icon {
      color: #409eff;
    }
  }
}

.about-content {
  .about-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid var(--app-border);

    &:last-child {
      border-bottom: none;
    }

    .label {
      font-size: 14px;
      color: var(--app-text-secondary);
    }

    .value {
      font-size: 14px;
      font-weight: 500;
      color: var(--app-text);
      text-align: right;
      word-break: break-word;
    }

    .value-link {
      color: var(--app-accent, #0f766e);
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

:deep(.el-slider__marks-text) {
  font-size: 11px;
}

// 加密设置样式
.encryption-status-card {
  background: var(--app-surface-muted);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;

  .status-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;

    .status-label {
      font-size: 14px;
      font-weight: 500;
    }
  }

  .status-detail {
    font-size: 13px;
    color: var(--app-text-secondary);
  }
}

.encryption-form {
  margin-top: 16px;
}

.encryption-actions {
  margin-top: 16px;
  display: flex;
  gap: 12px;

  .el-button {
    flex: 1;
  }
}

// 导出解密数据区域样式
.export-decrypt-section {
  margin-top: 16px;
}

.export-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;

  .el-button {
    flex: 1;
    min-width: 140px;
  }
}

.key-input {
  font-family: monospace;
}

// =====================
// 移动端样式
// =====================
.is-mobile {
  .header {
    padding: 0 16px;

    h2 {
      font-size: 16px;
    }
  }

  .el-main {
    padding: 0;
  }

  .settings-content {
    padding: 12px;
  }

  .setting-card {
    margin-bottom: 12px;

    :deep(.el-card__body) {
      padding: 16px;
    }

    .card-header {
      font-size: 14px;
    }
  }

  // 表单标签垂直布局
  :deep(.el-form-item) {
    flex-direction: column;
    align-items: flex-start;

    .el-form-item__label {
      width: 100% !important;
      text-align: left;
      padding-bottom: 8px;
    }

    .el-form-item__content {
      width: 100%;
    }
  }

  .form-tip {
    font-size: 11px;
  }

  .value-display {
    font-size: 12px;
    text-align: left;

    .recommend-hint {
      display: block;
      margin-left: 0;
      margin-top: 4px;
    }
  }

  .vip-info {
    flex-direction: column;
    gap: 8px;
  }

  .about-content .about-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  // 29.1 加密操作按钮组优化 - 移动端垂直布局全宽按钮
  .encryption-actions {
    flex-direction: column;
    gap: 8px;

    .el-button {
      width: 100%;
      margin-left: 0 !important;
    }
  }

  // 导出解密数据按钮组 - 移动端垂直布局
  .export-actions {
    flex-direction: column;
    gap: 8px;

    .el-button {
      width: 100%;
      min-width: unset;
      margin-left: 0 !important;
    }
  }

  // 29.3 时间选择器和数字输入框优化 - 全宽布局和触摸友好
  .el-time-picker,
  :deep(.el-time-picker) {
    width: 100% !important;
  }

  .el-input-number,
  :deep(.el-input-number) {
    width: 100% !important;
  }

  // 增加触摸友好的控件大小
  :deep(.el-input-number__decrease),
  :deep(.el-input-number__increase) {
    width: 40px;
    height: 40px;
  }

  :deep(.el-input-number .el-input__inner) {
    height: 40px;
    line-height: 40px;
  }

  // 时间选择器触摸优化
  :deep(.el-time-picker .el-input__inner) {
    height: 40px;
    line-height: 40px;
  }
}

// 代理运行状态指示器
.proxy-status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.probing-tag {
  animation: probing-blink 1.5s ease-in-out infinite;
}

@keyframes probing-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.flap-count {
  font-size: 12px;
  color: var(--app-text-secondary);
}
</style>
