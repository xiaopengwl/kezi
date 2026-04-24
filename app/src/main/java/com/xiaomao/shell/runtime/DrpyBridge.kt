package com.xiaomao.shell.runtime

import android.content.Context
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import com.google.gson.Gson
import com.google.gson.JsonElement
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
    context: Context,
    private val httpClient: OkHttpClient,
) {
    private val gson = Gson()
    private val prefs =
        context.applicationContext.getSharedPreferences(AppConfig.drpyLocalPrefsName, Context.MODE_PRIVATE)

    @JavascriptInterface
    fun request(requestJson: String): String {
        val root = JsonParser.parseString(requestJson).asJsonObject
        val url = root.get("url")?.asString.orEmpty()
        val options = if (root.has("options") && root.get("options").isJsonObject) {
            root.getAsJsonObject("options")
        } else {
            JsonObject()
        }
        return executeRequest(url, options)
    }

    @JavascriptInterface
    fun batchRequest(requestsJson: String): String {
        val result = requestsJson.takeIf { it.isNotBlank() }?.let { JsonParser.parseString(it).asJsonArray } ?: return "[]"
        val response = result.map { element ->
            val item = element.asJsonObject
            val url = item.get("url")?.asString.orEmpty()
            val options = item.optJsonObject("options") ?: JsonObject()
            executeRequest(url, options)
        }
        return gson.toJson(response)
    }

    private fun executeRequest(url: String, options: JsonObject): String {
        val method = options.get("method")?.asString?.uppercase().orEmpty().ifBlank { "GET" }
        val bodyElement = options.takeIf { it.has("body") }?.get("body") ?: options.get("data")
        val withHeaders = options.get("withHeaders")?.asBoolean ?: false
        val toHex = options.get("toHex")?.asBoolean ?: false
        val contentType = options.get("contentType")?.asString ?: "application/x-www-form-urlencoded"
        val bodyText = formatBody(bodyElement, options, contentType)

        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", AppConfig.defaultUserAgent)

        options.optJsonObject("headers")?.entrySet()?.forEach { entry ->
            requestBuilder.header(entry.key, entry.value.asString)
        }

        if (method == "POST") {
            requestBuilder.post(bodyText.toRequestBody(contentType.toMediaTypeOrNull()))
        } else {
            requestBuilder.get()
        }

        return httpClient.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code} ${response.message}")
            }
            val bodyBytes = response.body?.bytes() ?: ByteArray(0)
            val bodyString = if (toHex) {
                bodyBytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
            } else {
                val charset = response.body?.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
                String(bodyBytes, charset)
            }
            if (!withHeaders) {
                bodyString
            } else {
                val headers = response.headers.toMultimap().mapValues { (_, value) -> value.firstOrNull().orEmpty() }
                JsonObject().apply {
                    addProperty("body", bodyString)
                    add("headers", gson.toJsonTree(headers))
                }.toString()
            }
        }
    }

    private fun formatBody(bodyElement: JsonElement?, options: JsonObject, contentType: String): String {
        if (bodyElement == null || bodyElement.isJsonNull) return ""
        if (bodyElement.isJsonPrimitive) return bodyElement.asString
        val isForm =
            options.get("postType")?.asString == "form" ||
                options.optJsonObject("headers")?.entrySet()?.any {
                    it.key.equals("Content-Type", ignoreCase = true) &&
                        it.value.asString.startsWith("application/x-www-form-urlencoded")
                } == true ||
                contentType.startsWith("application/x-www-form-urlencoded")
        if (!isForm || !bodyElement.isJsonObject) {
            return gson.toJson(bodyElement)
        }
        return bodyElement.asJsonObject.entrySet().joinToString("&") { entry ->
            "${entry.key.urlEncode()}=${entry.value.asString.urlEncode()}"
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
    fun localGet(ruleKey: String, key: String, defaultValue: String): String {
        return prefs.getString("$ruleKey@$key", defaultValue) ?: defaultValue
    }

    @JavascriptInterface
    fun localSet(ruleKey: String, key: String, value: String): String {
        prefs.edit().putString("$ruleKey@$key", value).apply()
        return value
    }

    @JavascriptInterface
    fun localDelete(ruleKey: String, key: String): String {
        prefs.edit().remove("$ruleKey@$key").apply()
        return ""
    }

    @JavascriptInterface
    fun pd(html: String, parse: String, baseUrl: String): String {
        return HikerHtmlParser(baseUrl).pd(html, parse, baseUrl)
    }

    @JavascriptInterface
    fun pdfa(html: String, parse: String): String {
        return gson.toJson(HikerHtmlParser().pdfa(html, parse))
    }

    @JavascriptInterface
    fun pdfh(html: String, parse: String, baseUrl: String): String {
        return HikerHtmlParser(baseUrl).pdfh(html, parse, baseUrl)
    }

    @JavascriptInterface
    fun pdfl(html: String, parse: String, listText: String, listUrl: String, urlKey: String): String {
        return gson.toJson(HikerHtmlParser().pdfl(html, parse, listText, listUrl, urlKey))
    }

    @JavascriptInterface
    fun log(message: String) {
        Log.d("DrpyBridge", message)
    }

    private fun JsonObject.optJsonObject(name: String): JsonObject? {
        return if (has(name) && get(name).isJsonObject) getAsJsonObject(name) else null
    }

    private fun String.urlEncode(): String {
        return java.net.URLEncoder.encode(this, Charsets.UTF_8.name())
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
