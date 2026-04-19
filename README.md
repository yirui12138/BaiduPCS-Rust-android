# BaiduPCS-Rust Android

Android local port of [BaiduPCS-Rust](https://github.com/komorebiCarry/BaiduPCS-Rust), with a mobile-first UI, embedded Rust runtime, and source-level open-source compliance.

![License](https://img.shields.io/badge/license-Apache--2.0-blue)
![Android](https://img.shields.io/badge/platform-Android-green)
![Rust](https://img.shields.io/badge/backend-Rust-orange)
![Vue](https://img.shields.io/badge/frontend-Vue%203-42b883)

用户可见应用名：**柏渡云盘**

> 本项目是 `BaiduPCS-Rust v1.12.1` 的 Android 本地化移植版。它不是百度网盘官方客户端，也不是上游项目的官方发布版本。

## 这是什么

`BaiduPCS-Rust Android` 不是简单的 WebView 套壳。

它把上游 Rust 后端、Vue 前端和 Android 原生能力重新组织到一个移动端 App 里：Rust 服务在手机本地运行，前端资源内置在 APK 中，Android 壳层负责文件导入、剪贴板识别、VPN 状态、网页登录 Cookie 导入、目录打开兜底和运行时保活。

目标是让用户在 Android 上获得接近原生 App 的体验，而不是被迫自己部署服务器、复制路径、切换浏览器、反复解释“为什么这个按钮点了没反应”。

## 项目特色

| 方向 | 实现 |
| --- | --- |
| 本地运行 | Rust 后端随 App 在 Android 本机启动，无需外接服务器 |
| 移动端 UI | 底部导航、全面屏安全区、暗色模式、折叠设置页、移动端登录引导 |
| 登录体验 | 扫码登录状态可见，网页登录 Cookie 自动导入，保留手动 Cookie 兜底 |
| 上传体验 | Android 系统选择器导入到 App 专属目录，确认后自动创建上传任务 |
| 下载体验 | 默认公共 Download 路径，打开目录失败时回退到应用内浏览，避免闪退 |
| 分享转存 | 支持分享链接解析、提取码、剪贴板识别、转存后自动下载、任务管理 |
| 稳定性 | VPN 友好提示、前台运行保护、目录打开兼容策略、上传导入缓存管理 |
| 合规性 | 保留上游来源、Apache 2.0、NOTICE、修改声明、第三方依赖许可证资产 |

## 功能清单

### 账号登录

- 百度网盘 App 扫码登录。
- 显示二维码登录阶段：等待扫码、扫描成功、等待确认、同步会话、进入文件页。
- Android WebView 百度网页登录，并在用户登录完成后导入本机 Cookie。
- 手动 Cookie 粘贴作为高级备用方式。
- 会话持久化和登录状态检查。

### 文件管理

- 浏览网盘目录。
- 面包屑导航。
- 文件和文件夹列表。
- 文件下载入口。
- 文件分享入口。
- 移动端工具栏适配。
- 深色模式适配。

### 下载

- 单文件下载。
- 文件夹下载。
- 批量下载。
- 下载任务管理。
- 暂停、继续、删除。
- 下载进度、速度和状态展示。
- 默认下载到公共 Download 目录。
- 下载完成后打开目录。
- 系统文件管理器不可用时自动回退到应用内目录浏览。

### 上传

- 单文件上传。
- 文件夹上传。
- Android 系统选择器导入。
- 文件先复制到 App 专属路径，再创建上传任务。
- 卡片式确认上传目标目录。
- 上传任务自动入队。
- 上传进度、暂停、继续、重试、删除。
- 上传冲突策略。
- 导入缓存清理，降低存储占用。

### 分享与转存

- 百度网盘分享链接解析。
- 支持 `pan.baidu.com/s/...`。
- 支持 `pan.baidu.com/share/init?surl=...`。
- 支持 `pwd=xxxx`。
- 支持中文“提取码：xxxx”。
- 剪贴板分享链接识别。
- 设置中可关闭自动剪贴板识别。
- 分享文件预览。
- 选择分享内文件。
- 转存全部。
- 转存后自动下载。
- 转存任务管理。
- “分享与转存”整合页。

### Android 原生能力

- 文件导入。
- 文件夹导入。
- 剪贴板读取。
- VPN 状态检测。
- 百度网页登录 Cookie 导入。
- App 回到前台事件。
- 系统文件管理器打开目录。
- 打开目录失败兜底。
- 本地 Rust 服务启动和状态管理。

## 移动端体验细节

- 顶部预留 Android 状态栏安全区，避免头像和菜单被遮挡。
- 底部导航保持文件、下载、上传、分享、设置五个主入口。
- 设置页在移动端默认折叠，点击分区后再展开。
- 分享转存入口放在文件页工具栏，不占用全局标题栏。
- 扫码登录时提示“扫码后请回到本页等待片刻”，避免用户误以为扫码失败。
- VPN 只做小巧提醒，不阻止用户继续使用。

VPN 提示文案：

> 我们无意冒犯您的互联网自由，但本软件在vpn环境下尚不稳定，您依然可以使用本软件，但关闭vpn可以提升稳定性

## 隐私与安全边界

- 本项目不上传用户 Cookie 到第三方。
- 不在日志中打印完整 Cookie。
- WebView Cookie 导入需要用户主动打开网页登录流程。
- 上传导入文件存放在 App 专属路径，用于后续自动上传。
- 下载文件默认面向公共 Download 目录，方便用户查找和管理。
- 本开源仓库不包含账号数据、session、下载内容、运行时数据库、日志、签名密钥或 APK。

## 当前限制

- 本仓库是源码开源仓库，不直接提供 APK。
- 自动备份相关历史模块仍在源码中，但 Android 移动端入口已撤下，不作为当前 Android 主功能宣传。
- 百度网页登录可能受官方风控、页面调整或 WebView 兼容性影响，因此保留手动 Cookie 作为备用方式。
- Android ROM 文件管理器兼容性差异较大，打开目录会尽力唤醒系统文件管理器，失败时回退到应用内浏览。

## 技术栈

| 层 | 技术 |
| --- | --- |
| Android | Kotlin, Jetpack Compose, WebView |
| 后端 | Rust, Tokio, Axum |
| 前端 | Vue 3, TypeScript, Vite, Element Plus |
| 通信 | Local HTTP API, WebSocket, Android JS Bridge |
| 构建 | Gradle, Cargo, npm |
| 合规资产 | 自定义 open-source assets 生成脚本 |

## 目录结构

```text
.
├── android/                 # Android 壳层、WebView、原生桥接、Gradle 配置
├── backend/                 # Rust 本地服务和网盘核心能力
├── frontend/                # Vue 3 前端
├── decrypt-cli/             # 独立解密工具
├── docs/                    # 文档与图片资源
├── scripts/                 # 构建、许可证头、开源资产脚本
├── LICENSE                  # Apache License 2.0
├── NOTICE.txt               # 本移植版 NOTICE
├── MODIFIED_FROM_UPSTREAM.md
└── OPEN_SOURCE_PACKAGE_NOTES.md
```

## 从源码构建

### 环境要求

- Android Studio
- JDK 17
- Android SDK / NDK
- Rust toolchain
- Node.js 与 npm
- Gradle

### 构建前端

```bash
cd frontend
npm install
npm run build
```

### 检查 Rust 后端

```bash
cd backend
cargo check
```

### 构建 Android APK

```bash
cd android
gradle assembleRelease
```

如果需要自定义 Android Rust target 输出目录：

```bash
BAIDUPCS_ANDROID_TARGET_DIR=/path/to/android-rust-target
```

如果 Windows 主机构建需要额外 linker 或 native flags：

```bash
BAIDUPCS_HOST_LINKER=/path/to/rust-lld
BAIDUPCS_HOST_NATIVE_FLAGS="flag1;flag2"
```

## 开源合规

本项目基于 `BaiduPCS-Rust v1.12.1` 移植，并按 Apache License 2.0 分发。

已包含：

- [`LICENSE`](LICENSE)：Apache License 2.0 全文。
- [`NOTICE.txt`](NOTICE.txt)：本 Android 移植版 NOTICE。
- [`MODIFIED_FROM_UPSTREAM.md`](MODIFIED_FROM_UPSTREAM.md)：相对上游的修改说明。
- 源码许可证头：主要第一方源码文件包含许可证和修改声明。
- 应用内法律页：展示开源许可、上游来源、鸣谢和第三方依赖许可证信息。
- 构建期许可证资产：通过 [`scripts/generate_open_source_assets.py`](scripts/generate_open_source_assets.py) 生成第三方运行时依赖许可证资产。

上游信息：

- 上游项目：[`komorebiCarry/BaiduPCS-Rust`](https://github.com/komorebiCarry/BaiduPCS-Rust)
- 引用版本：[`v1.12.1`](https://github.com/komorebiCarry/BaiduPCS-Rust/releases/tag/v1.12.1)
- 上游作者：`komorebiCarry`

## 发布包说明

本仓库来自一个源码-only 开源副本。发布前已排除：

- APK / AAB
- `node_modules`
- Rust `target`
- Android / Gradle build 输出
- 下载文件
- 日志
- 运行时数据库
- 账号 Cookie / session
- 签名密钥
- 本机路径配置

详见 [`OPEN_SOURCE_PACKAGE_NOTES.md`](OPEN_SOURCE_PACKAGE_NOTES.md)。

## 贡献

欢迎提交 Issue 和 Pull Request。

如果改动涉及以下内容，请在 PR 中说明测试场景和风险：

- 登录、Cookie、二维码轮询。
- Android 系统权限。
- 上传导入和下载目录。
- 分享链接转存。
- 许可证文本、NOTICE、第三方依赖。
- 用户隐私或日志输出。

## 致谢

感谢：

- [`komorebiCarry/BaiduPCS-Rust`](https://github.com/komorebiCarry/BaiduPCS-Rust)
- Rust / Tokio / Axum
- Vue 3 / Vite / Element Plus
- Android / Kotlin / WebView
- Apache License 2.0 开源生态

也感谢所有认真反馈移动端体验问题的人。这个移植版一路上解决的许多问题，都是从“这里挡住了”“这个按钮太绕了”“登录太不确定了”这些非常真实的使用细节里磨出来的。
