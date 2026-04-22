# 晓鹏影视 Android

轻量版 drpy/t3-js 安卓影视壳，内置 WebView JS 执行引擎，可添加多个视频源并在线播放。

## 本版更新

- 软件名改为：**晓鹏影视**
- 首页 UI 重做：深色高级风格、顶部品牌区、分类胶囊、影视封面卡片
- 列表支持影视封面图加载；没有封面时显示高级渐变占位
- 源管理升级：支持添加、切换、保存、删除多个源
- 播放页接入 **ArtPlayer.js**，并内置 hls.js 支持 m3u8
- 继续支持 drpy/t3-js 源里的 `推荐 / 一级 / 搜索 / 二级 / lazy`

## 使用

1. 打开 App。
2. 首页点击右上角「源管理」。
3. 可粘贴完整 `var rule = {...}` 源。
4. 点击「新增源」添加新源，或「保存当前」覆盖当前源。
5. 点击源列表可切换源；「删除当前」可删除当前源。
6. 进入详情页后选择播放节点，会用 ArtPlayer 播放。

## GitHub Actions 打包

仓库已包含：

```text
.github/workflows/android-debug-apk.yml
```

打包方式：

1. 打开 GitHub 仓库。
2. 进入 `Actions`。
3. 选择 `Android Debug APK`。
4. 点击 `Run workflow`。
5. 成功后下载 `Artifacts -> app-debug-apk`。
6. 解压得到 `app-debug.apk`。

## 说明

这是轻量版 drpy 安卓壳。已实现常用流程，但如果某些源依赖特殊内置 API，后续需要在 `DrpyEngine.java` 继续补桥接函数。
