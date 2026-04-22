# 吃瓜 v32 安卓项目

这是根据 `chigua_t3_js_drpy_v32_playurl_fix.js` 迁移的原生 Android Java 项目，可直接用 Android Studio 打开。

## 已迁移能力

- 首页/推荐列表扫描 `/archives/`
- 24 个分类入口
- 搜索 `/search/关键词/页码/`
- 翻页
- 详情页解析：标题、简介、播放节点
- 播放地址提取：优先解析 `data-config` 里的 `.m3u8/.mp4`，失败时打开原详情页
- 内置 `VideoView` 播放；播放失败时自动调用外部播放器/浏览器
- 新增“源管理”：可以直接粘贴 `var rule = {...}` 这种 drpy/t3-js 源
- 新增 WebView JS 执行引擎：实际执行源里的 `推荐 / 一级 / 搜索 / 二级 / lazy`
- Java Bridge 提供 `request()`、`setResult()`、`input`、`rule` 等常用 drpy 运行环境

## 自定义添加源

1. 打开 App 首页。
2. 点右上角“源管理”。
3. 粘贴完整 `var rule = {...}` drpy/t3-js 源。
4. 点“保存源”，返回首页会自动按该源运行。

当前已支持这类源的核心流程：

- `class_name / class_url` 分类读取
- `推荐` 首页执行
- `一级` 分类执行
- `搜索` 执行
- `二级` 详情执行，读取 `VOD` 或 `setResult`
- `lazy` 播放地址解析，支持返回 `{url: ...}`

说明：这是轻量版 drpy 安卓壳，已能执行 JS 源的核心逻辑；如果某些源依赖更完整的道长/zyfun 内置 API，需要继续在 `DrpyEngine.java` 里补对应函数。

## 使用方法

### 方案 A：本地 Android Studio 运行

1. 用 Android Studio 打开本目录 `chigua-android-app`。
2. 等待 Gradle 同步。
3. 连接安卓手机，点击 Run。

### 方案 B：GitHub Actions 在线自动打包 APK（更省事）

如果你不想本地配 Android 环境，可以直接用仓库自带的 GitHub Actions：

1. 把整个 `chigua-android-app` 上传到一个 GitHub 仓库。
2. 确保仓库里包含这个文件：
   - `.github/workflows/android-debug-apk.yml`
3. 打开 GitHub 仓库页面：
   - `Actions` → `Android Debug APK` → `Run workflow`
4. 等待 3~10 分钟左右。
5. 运行成功后，在本次 workflow 的 `Artifacts` 里下载：
   - `app-debug-apk`
6. 解压后得到：
   - `app-debug.apk`

这个流程会在 GitHub 云端自动：

- 安装 JDK 17
- 安装 Android SDK 35
- 安装 Build Tools 35.0.0
- 执行 `gradle assembleDebug`
- 自动上传编译好的 debug APK

> 注意：首次上传到 GitHub 后，如果默认分支不是 `main/master`，也没关系，你仍然可以手动点 `Run workflow`。

## 说明

- 当前环境没有 Android SDK/Gradle，所以我已生成源码项目，但未在本机编译 APK。
- 这个版本没有引入第三方依赖，避免因为依赖下载失败导致导入报错。
- 如果目标站域名失效，可直接在 App 的“源管理”里粘贴新源，或在 `Scraper.java` 的 `HOSTS` / `SourceConfig.java` 里补域名。
