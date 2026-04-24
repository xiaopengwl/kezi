package com.xiaomao.shell.data

import android.content.Context
import com.xiaomao.shell.config.AppConfig

class SourceStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(AppConfig.prefsName, Context.MODE_PRIVATE)

    fun getSourceText(): String {
        val cached = prefs.getString(AppConfig.sourcePrefsKey, null)
        return if (cached.isNullOrBlank()) {
            // 首次启动自动写入内置示例源，保证应用开箱可测。
            readDefaultSource().also { saveSourceText(it) }
        } else {
            cached
        }
    }

    fun saveSourceText(sourceText: String) {
        prefs.edit().putString(AppConfig.sourcePrefsKey, sourceText).apply()
    }

    fun resetToDefault(): String {
        val content = readDefaultSource()
        saveSourceText(content)
        return content
    }

    fun readDefaultSource(): String {
        return appContext.assets.open(AppConfig.sourceAssetPath).bufferedReader().use { it.readText() }
    }
}
