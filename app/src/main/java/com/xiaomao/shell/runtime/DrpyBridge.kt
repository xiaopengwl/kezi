package com.xiaomao.shell.runtime

import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.xiaomao.shell.config.AppConfig
import java.net.URL
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class DrpyBridge(
    private val httpClient: OkHttpClient,
) {
    @JavascriptInterface
    fun request(requestJson: String): String {
        val root = JsonParser.parseString(requestJson).asJsonObject
        val url = root.get("url")?.asString.orEmpty()
        val options = if (root.has("options") && root.get("options").isJsonObject) {
            root.getAsJsonObject("options")
        } else {
            JsonObject()
        }

        val method = options.get("method")?.asString?.uppercase().orEmpty().ifBlank { "GET" }
        val bodyText = options.get("body")?.asString ?: options.get("data")?.asString.orEmpty()

        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", AppConfig.defaultUserAgent)

        options.optJsonObject("headers")?.entrySet()?.forEach { entry ->
            requestBuilder.header(entry.key, entry.value.asString)
        }

        if (method == "POST") {
            val contentType = options.get("contentType")?.asString ?: "application/x-www-form-urlencoded"
            requestBuilder.post(bodyText.toRequestBody(contentType.toMediaTypeOrNull()))
        } else {
            requestBuilder.get()
        }

        return httpClient.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code} ${response.message}")
            }
            response.body?.string().orEmpty()
        }
    }

    @JavascriptInterface
    fun joinUrl(base: String, relative: String): String {
        return try {
            URL(URL(base), relative).toString()
        } catch (_: Throwable) {
            relative
        }
    }

    @JavascriptInterface
    fun base64Encode(content: String): String {
        return Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    @JavascriptInterface
    fun base64Decode(content: String): String {
        return String(Base64.decode(content, Base64.DEFAULT), Charsets.UTF_8)
    }

    @JavascriptInterface
    fun log(message: String) {
        Log.d("DrpyBridge", message)
    }

    private fun JsonObject.optJsonObject(name: String): JsonObject? {
        return if (has(name) && get(name).isJsonObject) getAsJsonObject(name) else null
    }

    companion object {
        fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(AppConfig.httpTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(AppConfig.httpTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(AppConfig.httpTimeoutSeconds, TimeUnit.SECONDS)
                .build()
        }
    }
}
