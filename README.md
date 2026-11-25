# 安心浏览器 · Android 客户端（v0.1 骨架）

基于《安心浏览器：技术选型与开发实施方案》与 `ui.png` 设计稿落地的第一版可运行工程。

当前版本的目标是**把"浏览器外壳 + 业务骨架"先跑通**：UI 全部按液态玻璃设计稿实现，
浏览器内核以 Android WebView 作为第一阶段的临时实现，并通过统一接口与上层解耦，
后续替换为 Titanium / Chromium 时不需要改动业务与界面代码。

## 一、已实现内容

| 页面 | 说明 |
| --- | --- |
| 首页 | 壁纸 Hero、玻璃搜索栏、快捷网站、今日推荐、快捷工具、媒体速览 |
| 标签页 | 全部 / 无痕分组、标签卡片、新建与关闭、空状态 |
| 扩展中心 | 分类切换、推荐横幅、扩展安装 / 启停、本地导入入口 |
| 下载管理 | 下载中 / 已完成 / 资源库、进度、暂停、清空记录 |
| 播放器 | Media3 ExoPlayer + HLS、自绘玻璃控制层、倍速、画中画、下载入口 |
| 设置（我的） | 账号卡、广告过滤开关、隐私 / 无痕 / 扩展 / 下载等入口 |
| 无痕浏览 | 深色液态玻璃页、隐私三提示、一键进入无痕标签 |
| 网页浏览 | 真实 WebView 浏览、地址栏编辑、进度条、菜单面板、返回键接管 |
| AI 助手 | 会话式界面骨架（模型服务待接入） |
| 媒体嗅探 | 页面层脚本嗅探 video/audio/媒体直链，写入统一资源模型 |

功能层面可行的部分都是真实可用的：网页浏览、下载（系统 DownloadManager）、
播放器（ExoPlayer）、设置持久化（DataStore）、标签管理（多 WebView 实例）。

运行截图见 `docs/screenshots/`（Pixel 9 1080×2424 实机模拟器截取）：
`01-home` 首页、`02-tabs` 标签页、`03-extensions` 扩展中心、`04-downloads` 下载管理、
`05-settings` 设置、`06-incognito` 无痕浏览、`07-player` 播放器。

## 二、工程结构

```
anxin-browser/
├── app/                      # 应用入口、导航、各功能页面
│   └── src/main/java/com/anxin/browser/
│       ├── MainActivity.kt
│       ├── AppGraph.kt               # 轻量服务定位器（可换 Hilt）
│       ├── nav/AnxinNavHost.kt       # 路由与页面装配
│       ├── ui/Components.kt          # 底部玻璃导航栏等公共 UI
│       ├── data/                     # SettingsStore / ExtensionStore
│       └── feature/                  # home / tabs / extensions / downloads
│                                     # player / settings / incognito / ai / browser
├── core/
│   ├── design/               # 液态玻璃设计系统（主题、玻璃面板、极光背景）
│   └── browser/              # 内核抽象层：BrowserEngine、WebViewEngine、
│                             # TabManager 模型、媒体资源模型、下载仓库
└── artwork/                  # 生成的设计素材原图（壁纸 / 视频首帧）
```

### 内核解耦约定

`core/browser` 只暴露 `BrowserEngine` 接口：

```kotlin
createTab() / closeTab() / selectTab()
loadUrl() / goBack() / goForward() / reload() / stopLoading()
clearBrowsingData() / clearMediaResources() / destroy()
```

`WebViewEngine` 是当前实现。接入 Titanium 时新增 `ChromiumEngine : BrowserEngine`，
在 `AppGraph` 中替换一行即可，UI 与业务层零改动。

## 三、构建与运行

```bash
# 环境：JDK 17、Android SDK Platform 34 + Build-Tools 34.0.0
# sdk 路径写在 local.properties（该文件不入库）

./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk

./gradlew :app:installDebug   # 已连接设备时直接安装
```

依赖通过阿里云 Maven 镜像拉取（`settings.gradle.kts` 已配置），
在无法访问 Google Maven 的网络环境也能构建。

## 四、当前边界（有意保留的缺口）

1. **Chrome 扩展不能真正运行**：WebView 没有 Extension Runtime。扩展中心的安装、
   启停、本地 ZIP/CRX 导入均为管理入口，等 Chromium 底座接入后接管。
2. **无痕隔离是近似实现**：不写缓存、不记历史、退出清理，但 Cookie 未做进程级隔离；
   Chromium 的 OTR Profile 才能做到真正的数据分离。
3. **资源嗅探仅覆盖页面层**：注入脚本监听 `video/audio/source` 与媒体直链；
   HLS/DASH 分片合并、内核网络观察需要 Chromium 网络栈支持。
4. **AI 助手为界面骨架**：会话、建议、输入链路已通，未接入模型服务。
5. 下载中心在没有真实任务时展示演示数据（页面内已标明），真实下载会替换演示列表。

## 五、下一步建议（对应方案文档阶段二 / 三）

- 用 Titanium 编译出首个 APK，先跑通方案里的 9 项内核验证清单，再决定底座。
- 内核接口保持不变的前提下，补 `TabPersistence`（会话恢复）与书签/历史 Room 表。
- 把 `MediaSniffer` 从页面脚本升级为独立模块，接入内核网络观察 + HLS/DASH 解析。
- UI 侧补充：菜单面板、书签页、历史页、设置二级页。
