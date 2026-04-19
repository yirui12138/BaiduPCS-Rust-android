# 柏渡云盘 Android 版

![License](https://img.shields.io/badge/license-Apache--2.0-blue)
![Android](https://img.shields.io/badge/platform-Android%2010+-green)
![Architecture](https://img.shields.io/badge/架构-本地运行-purple)

> **应用名：柏渡云盘**

**柏渡云盘** 是百度网盘的第三方 Android 客户端，所有功能在本地运行，无需搭建服务器，开箱即用。

本项目基于 [BaiduPCS-Rust v1.12.1](https://github.com/komorebiCarry/BaiduPCS-Rust) 移植，采用 Apache License 2.0 开源协议。\
您可以于releases下载发行版体验

***

## 本项目复现了上游项目以下功能：

### 完全本地运行

不需要服务器，不需要配置，安装即可使用。Rust 核心服务直接运行在手机上，数据不上传第三方。

### 三种登录方式

| 方式       | 特点                  |
| -------- | ------------------- |
| **扫码登录** | 百度网盘 App 扫一扫，最方便    |
| **网页登录** | 内置浏览器登录，自动提取 Cookie |
| **手动粘贴** | 备用方案，适合高级用户         |

### 分享链接一键转存

- 自动识别剪贴板中的分享链接
- 支持提取码自动填充
- 转存后可选择自动下载
- 任务进度实时可见

### 下载体验优化

- 多线程加速下载
- 断点续传，不怕中断
- 文件夹批量下载自动打包
- 下载完成自动唤醒文件管理器

### 上传更省心

- 秒传功能：相同文件瞬间完成
- 系统文件选择器直接选取
- 上传冲突智能处理（覆盖/跳过/重命名）
- 自动清理导入缓存，不占用空间

### 隐私加密保护

- 客户端加密，文件内容只有你能解密
- 文件名也可加密，保护隐私
- 密钥支持导出备份，防止丢失

<br />

***

## 本项目自行添加了如下功能

1.安卓原生适配与优化\
2.添加了更符合手机用户使用习惯的登录方式\
3.在首页添加便捷的转存胶囊，可以自动解析分析链接并方便地转存下载\
4.vpn提示，本功能仅处于app在vpn环境下的不稳定，不强制要求用户关闭vpn，本项目尊重每个人的互联网自由

***

## 为什么选择柏渡云盘？

| 对比项   | 柏渡云盘          | 其他第三方工具         |
| ----- | ------------- | --------------- |
| 部署难度  | 安装即用，零配置      | 需要服务器或复杂配置      |
| 数据安全  | 本地运行，数据不上传    | 可能依赖第三方服务器      |
| 登录方式  | 扫码+网页+手动，灵活选择 | 通常只支持 Cookie 粘贴 |
| 移动端体验 | 原生 Android 适配 | 多为网页版或简单套壳      |
| 开源合规  | 完整许可证和来源声明    | 合规性参差不齐         |

***

## 快速开始

### 环境要求

- Android 10 (API 26) 及以上
- 无需 Root

### 从源码构建

```bash
# 1. 构建前端
cd frontend
npm install
npm run build

# 2. 构建 Android APK
cd ../android
./gradlew assembleRelease
```

详细构建说明见下方 [构建指南](#构建指南)。

***

## 开源合规

本项目严格遵守开源协议：

- **许可证**: Apache License 2.0
- **上游项目**: [BaiduPCS-Rust](https://github.com/komorebiCarry/BaiduPCS-Rust) by komorebiCarry
- **引用版本**: v1.12.1

合规文件：

- [LICENSE](LICENSE) - Apache 2.0 完整文本
- [NOTICE.txt](NOTICE.txt) - 移植版声明
- [MODIFIED\_FROM\_UPSTREAM.md](MODIFIED_FROM_UPSTREAM.md) - 修改说明

源代码均包含 SPDX 许可证头，第三方依赖许可证在构建时自动生成。

***

## 构建指南

### 环境准备

| 工具             | 版本要求          |
| -------------- | ------------- |
| Android Studio | 最新稳定版         |
| JDK            | 17            |
| Android SDK    | API 34        |
| Android NDK    | 26.3.11579264 |
| Rust           | 最新稳定版         |
| Node.js        | 18+           |

### 构建步骤

```bash
# 构建前端
cd frontend
npm install
npm run build

# 检查 Rust 后端
cd ../backend
cargo check

# 构建 APK
cd ../android
./gradlew assembleRelease
```

### Windows 环境变量

```bash
set BAIDUPCS_ANDROID_TARGET_DIR=F:\custom\path
set BAIDUPCS_HOST_LINKER=F:\path\to\linker
```

***

## 技术架构

```
┌─────────────────────────────────────┐
│         Android 应用层               │
│  (Kotlin + Jetpack Compose + WebView)│
└─────────────┬───────────────────────┘
              │ HTTP + WebSocket
┌─────────────▼───────────────────────┐
│         Rust 核心服务                │
│  (下载/上传/加密/网盘 API/数据存储)    │
└─────────────────────────────────────┘
```

- **后端**: Rust + Tokio + Axum
- **前端**: Vue 3 + TypeScript + Vite
- **原生层**: Kotlin + Jetpack Compose

***

## 注意事项

1. **非官方客户端**: 本应用与百度公司无关，亦非<https://github.com/komorebiCarry/BaiduPCS-Rust>的官方发行版
2. **数据本地存储**: Cookie、下载文件等仅保存在本地，不会上传服务器
3. **自动备份**: 上游项目的实现方式难以在安卓系统复现，故短期无开发计划

***

## 贡献

欢迎提交 Issue 和 Pull Request，你们的反馈将使项目变得更好

***

## 致谢

- [komorebiCarry/BaiduPCS-Rust](https://github.com/komorebiCarry/BaiduPCS-Rust) - 该项目的高可拓展性与宽松开源协议为移植提供了可靠底座
- Rust / Vue / Android 开源社区
- 所有反馈使用体验的用户的用户

***

<div align="center">

**柏渡云盘** | Apache License 2.0 | 2026 Android Port Contributors

</div>
