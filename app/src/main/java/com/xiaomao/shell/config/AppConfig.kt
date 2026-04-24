package com.xiaomao.shell.config

object AppConfig {
    // 默认源、运行时资产和网络参数统一集中到这里，后续替换规则时只需改这一处。
    const val prefsName = "xiaomao_shell_prefs"
    const val sourcePrefsKey = "source_text"
    const val sourceAssetPath = "sources/zyfun-site.json"
    const val runtimeHtmlPath = "runtime/runtime_host.html"
    const val defaultRemoteSourceUrl =
        "https://raw.githubusercontent.com/xiaopengwl/kezi/main/app/src/main/assets/sources/zyfun-site.json"
    const val defaultUserAgent =
        "Mozilla/5.0 (Linux; Android 14; XiaomaoShell) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    const val httpTimeoutSeconds = 20L
    const val homeCategoryId = "__home__"
    const val drpyLocalPrefsName = "xiaomao_drpy_local"
}
