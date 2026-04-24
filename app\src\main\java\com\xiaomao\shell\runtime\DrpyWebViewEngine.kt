package com.xiaomao.shell.runtime

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.xiaomao.shell.config.AppConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.coroutines.resume

class DrpyWebViewEngine(
    context: Context,
    private val bridge: DrpyBridge,
) {
    private val appContext = context.applicationContext
    private val webViewDeferred = CompletableDeferred<WebView>()
    private val readyDeferred = CompletableDeferred<Unit>()
    private val mutex = Mutex()

    init {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            createWebView()
        } else {
            runBlocking(Dispatchers.Main.immediate) {
                createWebView()
            }
        }
    }

    suspend fun execute(
        sourceText: String,
        action: String,
        input: String,
        page: Int,
    ): String = mutex.withLock {
        // 通过隐藏 WebView 执行 xiaomaojs / drpy 规则，兼容 JS 字符串脚本能力。
        val webView = webViewDeferred.await()
        readyDeferred.await()
        val payload = org.json.JSONObject().apply {
            put("source", sourceText)
            put("action", action)
            put("input", input)
            put("page", page)
        }.toString()

        withContext(Dispatchers.Main.immediate) {
            val script = "window.__nativeRuleRuntime.execute(${org.json.JSONObject.quote(payload)});"
            decodeJsString(awaitJs(webView, script))
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView() {
        val webView = WebView(appContext)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.userAgentString = AppConfig.defaultUserAgent
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                readyDeferred.complete(Unit)
            }
        }
        webView.addJavascriptInterface(bridge, "AndroidBridge")
        webView.loadUrl("file:///android_asset/${AppConfig.runtimeHtmlPath}")
        webViewDeferred.complete(webView)
    }

    private suspend fun awaitJs(webView: WebView, script: String): String {
        return suspendCancellableCoroutine { continuation ->
            webView.evaluateJavascript(script, ValueCallback { value ->
                if (continuation.isActive) {
                    continuation.resume(value ?: "null")
                }
            })
        }
    }

    private fun decodeJsString(raw: String): String {
        if (raw == "null") return ""
        return JSONArray("[$raw]").getString(0)
    }
}
