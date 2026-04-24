# XiaomaoJsShell

一个原生 Kotlin Android Studio 项目，用于对接 `xiaomaojs` 影视源规则，默认包含：

- 首页分类
- 分类列表
- 搜索
- 详情页
- 选集与线路切换
- ExoPlayer 播放 `m3u8`
- GitHub Actions 自动编译 APK

## 项目特点

- 纯本地播放壳
- 无广告、无后台服务、无多余权限
- 规则执行基于本地 `WebView + JavascriptInterface`
- 默认内置 `xiaomaojs` 示例源，可在应用内直接修改
- 接口地址与默认源入口集中在 `AppConfig.kt`

## 目录结构

```text
.
├── .github/workflows/build-apk.yml
├── app
│   ├── build.gradle.kts
│   └── src/main
│       ├── AndroidManifest.xml
│       ├── assets
│       │   ├── runtime
│       │   └── sources
│       ├── java/com/xiaomao/shell
│       └── res
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

## 本地打开方式

1. 用 Android Studio 打开项目根目录。
2. 首次同步 Gradle。
3. 运行 `app` 模块即可。

## GitHub Actions 自动打包

仓库已包含 [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml)，默认在每次 `push` 和手动触发时自动编译 `debug APK` 并上传构建产物。

如果你后续把签名证书和仓库信息给我，我可以继续补：

- Release 签名打包
- 自动创建 Release
- 自动上传到 GitHub Releases
