# BaiduPCS-Rust Android

> 基于 `BaiduPCS-Rust v1.12.1` 的 Android 本地化移植版。  
> 用户可见应用名：**柏渡云盘**。

本项目把上游 Rust + Vue 的百度网盘客户端移植到 Android App 形态：在手机上内置本地 Rust 服务、WebView 前端和 Android 系统能力，不需要用户额外自建服务器，也不需要外接桌面浏览器。

项目重点不是简单“套壳”，而是围绕移动端重新整理了登录、文件管理、上传下载、分享转存、系统文件访问、暗色模式、开源合规和 Android 稳定性。

## 重要声明

- 本项目是独立 Android 移植版，**不是百度网盘官方客户端**。
- 本项目不是上游 `BaiduPCS-Rust` 的官方发布版本。
- 本项目不包含任何账号 Cookie、下载内容、APK 构建产物、签名密钥或个人运行数据。
- 使用本项目需要遵守百度网盘相关服务条款及所在地法律法规。

## 项目来源

- 上游项目：[`komorebiCarry/BaiduPCS-Rust`](https://github.com/komorebiCarry/BaiduPCS-Rust)
- 引用版本：[`v1.12.1`](https://github.com/komorebiCarry/BaiduPCS-Rust/releases/tag/v1.12.1)
- 上游作者：`komorebiCarry`
- 本移植版许可证：Apache License 2.0，详见 [`LICENSE`](LICENSE)
- 本移植版 NOTICE：详见 [`NOTICE.txt`](NOTICE.txt)
- 修改说明：详见 [`MODIFIED_FROM_UPSTREAM.md`](MODIFIED_FROM_UPSTREAM.md)

## 核心特色

### Android 本地运行

- Rust 后端随 App 本地启动，手机端直接访问本机服务。
- Vue 3 前端打包进 APK assets，通过 WebView 加载。
- 不要求用户部署 NAS、VPS 或外部 Web 服务。
- Android 壳层负责运行时启动、资源释放、前台保活、系统能力桥接。

### 移动端优先 UI

- 文件、下载、上传、分享、设置五个底部主入口。
- 顶部安全区、全面屏、状态栏、底部导航适配。
- 深色模式和浅色模式均做了可读性优化。
- 设置页在移动端使用折叠长条，减少滚动负担。
- 分享转存入口集成在文件页工具栏，不遮挡标题和文件列表。

### 登录体验优化

- 扫码登录支持明确阶段提示：
  - 生成二维码中
  - 等待扫码
  - 扫描成功，等待手机确认
  - 授权成功，正在同步登录
  - 登录完成，正在进入文件页
- 扫码后会提示用户回到 App 等待片刻，减少“是不是没扫到”的困惑。
- 支持网页登录导入 Cookie：
  - 用户主动打开百度网盘网页登录页。
  - 登录完成后读取本机 WebView Cookie 用于本应用登录。
  - 不把 Cookie 上传到第三方。
  - 不在日志中打印完整 Cookie。
- 保留手动 Cookie 粘贴作为高级备用方式。

### 上传链路移动化

- 上传入口统一收口到上传页。
- Android 端上传流程为：
  - 点击上传
  - 选择文件或文件夹
  - 复制到 App 专属路径
  - 卡片式确认网盘目标目录
  - 自动创建上传任务
- 上传完成后可清理导入缓存，降低手机存储压力。
- 支持上传任务进度、暂停、继续、重试、删除。

### 下载链路稳定化

- 默认下载目录面向公共 Download 目录。
- 下载完成后支持打开目录。
- 系统文件管理器打开失败时会回退到应用内目录浏览，避免因为 ROM 兼容问题导致闪退。
- 下载任务支持进度、速度、暂停、继续、删除和完成态管理。

### 分享与转存

- 新增“分享与转存”移动端页面，将转存任务和我的分享整合在一起。
- 文件页工具栏提供分享转存入口。
- 支持手动输入百度网盘分享链接。
- 支持从剪贴板识别百度网盘分享链接，识别到后提示“识别到分享，请点击转存”。
- 自动剪贴板识别可在设置中关闭。
- 支持提取码解析与验证。
- 支持转存后自动下载。
- 支持转存任务管理。

### VPN 友好提示

Android 端会探测 VPN 网络状态。检测到 VPN 时，仅显示小巧提示：

> 我们无意冒犯您的互联网自由，但本软件在vpn环境下尚不稳定，您依然可以使用本软件，但关闭vpn可以提升稳定性

这是稳定性提醒，不是强制拦截。

### 开源合规内置

- App 内有“开源许可与鸣谢”页面。
- 包含上游来源、Apache 2.0 许可证、NOTICE、第三方依赖许可证清单。
- APK 构建会生成 open-source assets，用于应用内展示许可证信息。
- 源码文件补充了统一许可证头和修改声明。

## 功能实现清单

### 账号与会话

- 二维码扫码登录
- 扫码状态轮询与可视化反馈
- 登录成功后的会话同步与自动跳转
- WebView 百度网页登录导入 Cookie
- 手动 Cookie 登录备用入口
- 会话持久化
- 登录状态检查与失效处理

### 文件管理

- 网盘目录浏览
- 面包屑导航
- 文件夹进入
- 文件下载
- 文件分享
- 分享直下相关能力保留
- 移动端工具栏精简
- 深浅色模式适配

### 下载

- 单文件下载
- 文件夹下载
- 批量下载
- 下载任务管理
- 暂停、继续、删除
- 下载完成打开目录
- 打开目录失败时应用内兜底
- 公共 Download 路径适配
- 下载进度、速度、状态展示

### 上传

- 单文件上传
- 文件夹上传
- Android 系统选择器导入
- App 专属目录中转
- 卡片式确认上传
- 自动入队
- 上传任务管理
- 暂停、继续、重试、删除
- 上传冲突策略
- 上传缓存清理管理

### 分享与转存

- 百度网盘分享链接解析
- `pan.baidu.com/s/...` 链接识别
- `pan.baidu.com/share/init?surl=...` 链接识别
- `pwd=xxxx` 提取码识别
- 中文“提取码：xxxx”识别
- 剪贴板分享链接识别
- 设置中关闭自动剪贴板识别
- 分享文件预览
- 选择分享内文件
- 转存全部
- 转存后自动下载
- 转存任务状态管理
- 我的分享管理整合页

### 移动端体验

- 全面屏安全区适配
- 状态栏与顶部栏间距优化
- 底部导航
- 暗色模式
- 设置页折叠分区
- 触摸态优化，减少 WebView 默认蓝色点击框
- 小巧 VPN 提示弹窗
- 文件页分享转存工具栏入口
- 登录页移动端引导

### Android 原生桥接

- 文件导入
- 文件夹导入
- 打开系统文件管理器
- 打开目录失败回退
- 剪贴板读取
- VPN 状态检测
- 百度网页登录 Cookie 导入
- App 回前台事件派发
- 本地 Rust 服务启动与状态管理

### 安全、隐私与存储

- 不上传用户 Cookie 到第三方。
- 不在日志中打印完整 Cookie。
- 上传导入文件进入 App 专属路径后再入队。
- 下载位置面向公共 Download，方便用户查找。
- 导入缓存与运行时数据不进入开源包。
- 签名文件、账号文件、数据库、日志、APK 均不进入本仓库。

### 当前不作为 Android 主功能宣传的能力

上游和历史代码中保留了自动备份相关模块，但 Android 移动端前端入口已经撤下。原因是自动备份依赖长期后台运行、文件系统监听和系统调度，在 Android 上需要更严格的权限、保活和用户预期设计。本移植版当前不把自动备份作为 Android 可用主功能宣传。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| Android 壳层 | Kotlin, Jetpack Compose, WebView |
| 后端核心 | Rust, Axum, Tokio |
| 前端 | Vue 3, Vite, TypeScript, Element Plus |
| 通信 | 本地 HTTP API, WebSocket, Android JS Bridge |
| 构建 | Gradle, Cargo, npm |
| 合规资产 | 自定义 open-source assets 生成脚本 |

## 目录结构

```text
.
├── android/                 # Android App 壳层、WebView、原生桥接、Gradle 配置
├── backend/                 # Rust 本地服务、网盘能力、下载/上传/转存核心
├── frontend/                # Vue 3 移动端/桌面端前端
├── decrypt-cli/             # 独立解密工具源码
├── docs/                    # 项目文档与图片
├── scripts/                 # 构建、许可证、合规资产生成脚本
├── LICENSE                  # Apache License 2.0
├── NOTICE.txt               # 本移植版 NOTICE
├── MODIFIED_FROM_UPSTREAM.md
└── OPEN_SOURCE_PACKAGE_NOTES.md
```

## 构建说明

### 基础要求

- Android Studio
- JDK 17
- Android SDK / NDK
- Rust toolchain
- Node.js 与 npm
- Gradle

### 前端构建

```bash
cd frontend
npm install
npm run build
```

### Rust 后端检查

```bash
cd backend
cargo check
```

### Android APK 构建

```bash
cd android
gradle assembleRelease
```

如果需要自定义 Android Rust target 输出目录，可设置：

```bash
BAIDUPCS_ANDROID_TARGET_DIR=/path/to/android-rust-target
```

如果 Windows 主机构建需要额外 host linker 或 native flags，可设置：

```bash
BAIDUPCS_HOST_LINKER=/path/to/rust-lld
BAIDUPCS_HOST_NATIVE_FLAGS="flag1;flag2"
```

> 具体构建环境可能因 Android SDK、NDK、Rust target 和 Gradle 缓存状态不同而有所差异。

## 开源发布包说明

本仓库是源码开源副本，发布前已排除：

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

## 合规性

本项目按 Apache License 2.0 分发，并额外做了以下合规收口：

- 保留上游项目来源、作者和版本信息。
- 保留 Apache License 2.0 全文。
- 增加本移植版 NOTICE。
- 增加修改来源登记。
- 主要第一方源码文件包含许可证头。
- APK 法律页可展示开源许可与鸣谢。
- 第三方运行时依赖通过构建脚本生成许可证资产。
- 用户可见品牌与法律来源分离，避免暗示官方身份。

相关文件：

- [`LICENSE`](LICENSE)
- [`NOTICE.txt`](NOTICE.txt)
- [`MODIFIED_FROM_UPSTREAM.md`](MODIFIED_FROM_UPSTREAM.md)
- [`scripts/generate_open_source_assets.py`](scripts/generate_open_source_assets.py)
- [`scripts/apply_license_headers.py`](scripts/apply_license_headers.py)

## 品牌与命名

- 仓库名 `BaiduPCS-Rust-android` 用于说明本项目来源和技术脉络。
- App 用户可见名称为 **柏渡云盘**。
- `BaiduPCS-Rust`、`komorebiCarry` 等名称仅用于上游来源、许可证、鸣谢和兼容说明。

## 贡献

欢迎提交 Issue 和 Pull Request。建议贡献前先阅读：

- [`CONTRIBUTING.md`](CONTRIBUTING.md)
- [`MODIFIED_FROM_UPSTREAM.md`](MODIFIED_FROM_UPSTREAM.md)
- [`OPEN_SOURCE_PACKAGE_NOTES.md`](OPEN_SOURCE_PACKAGE_NOTES.md)

如果提交涉及登录、Cookie、下载路径、Android 系统权限、许可证文本或第三方依赖，请在 PR 中说明测试场景和合规影响。

## 致谢

感谢以下项目与技术生态：

- [`komorebiCarry/BaiduPCS-Rust`](https://github.com/komorebiCarry/BaiduPCS-Rust)
- Rust / Tokio / Axum
- Vue 3 / Vite / Element Plus
- Android / Kotlin / WebView
- Apache License 2.0 开源生态

也感谢所有愿意把问题说清楚、把体验磨细的使用者。移动端移植很容易“能跑就算”，但这个项目的目标是更进一步：尽量让它像一个真正可长期使用的移动产品。
