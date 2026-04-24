package com.xiaomao.shell.data

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.xiaomao.shell.config.AppConfig
import com.xiaomao.shell.data.model.Category
import com.xiaomao.shell.data.model.HomePage
import com.xiaomao.shell.data.model.PlayEpisode
import com.xiaomao.shell.data.model.PlayGroup
import com.xiaomao.shell.data.model.PlayResult
import com.xiaomao.shell.data.model.SourceMeta
import com.xiaomao.shell.data.model.VideoDetail
import com.xiaomao.shell.data.model.VideoItem
import com.xiaomao.shell.runtime.DrpyWebViewEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class RuleRepository(
    private val sourceStore: SourceStore,
    private val engine: DrpyWebViewEngine,
    private val httpClient: OkHttpClient,
) {
    // 首页、分类、搜索、详情和播放解析统一从规则引擎取数据，界面层不直接关心 JS 细节。
    fun currentSourceText(): String = sourceStore.getSourceText()

    fun saveSourceText(sourceText: String) {
        sourceStore.saveSourceText(sourceText)
    }

    fun resetSourceText(): String = sourceStore.resetToDefault()

    suspend fun syncDefaultRemoteSource(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(AppConfig.defaultRemoteSourceUrl)
            .header("User-Agent", AppConfig.defaultUserAgent)
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("远程同步失败：HTTP ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) {
                error("远程规则内容为空")
            }
            sourceStore.saveSourceText(body)
            body
        }
    }

    suspend fun loadHome(): HomePage {
        val source = sourceStore.getSourceText()
        val response = execute(source, "home")
        val meta = parseMeta(response.getAsJsonObject("meta"))
        val videos = parseVideos(response.getAsJsonArray("list"))
        return HomePage(meta = meta, videos = videos)
    }

    suspend fun loadCategory(categoryId: String, page: Int): List<VideoItem> {
        val source = sourceStore.getSourceText()
        if (categoryId == AppConfig.homeCategoryId) {
            return loadHome().videos
        }
        val response = execute(source, "category", input = categoryId, page = page)
        return parseVideos(response.getAsJsonArray("list"))
    }

    suspend fun search(keyword: String, page: Int): List<VideoItem> {
        val source = sourceStore.getSourceText()
        val response = execute(source, "search", input = keyword, page = page)
        return parseVideos(response.getAsJsonArray("list"))
    }

    suspend fun loadDetail(detailUrl: String): VideoDetail {
        val source = sourceStore.getSourceText()
        val response = execute(source, "detail", input = detailUrl)
        return parseDetail(response.getAsJsonObject("detail"))
    }

    suspend fun parsePlay(flag: String, playUrl: String): PlayResult {
        val source = sourceStore.getSourceText()
        val response = execute(source, "lazy", input = playUrl, flag = flag)
        val result = response.getAsJsonObject("play")
        return PlayResult(
            url = result.optString("url"),
            parse = result.optBoolean("parse"),
            jx = result.optInt("jx"),
            headers = result.optJsonObject("headers")?.entrySet()?.associate { it.key to it.value.asString }
                .orEmpty(),
        )
    }

    private suspend fun execute(
        sourceText: String,
        action: String,
        input: String = "",
        page: Int = 1,
        flag: String = "",
    ): JsonObject {
        val raw = engine.execute(sourceText = sourceText, action = action, input = input, page = page, flag = flag)
        val root = JsonParser.parseString(raw).asJsonObject
        if (!root.optBoolean("ok")) {
            val message = root.optString("error").ifBlank { "未知错误" }
            Log.e("RuleRepository", "execute failed: $message")
            throw IllegalStateException(message)
        }
        return root.getAsJsonObject("data")
    }

    private fun parseMeta(metaObject: JsonObject): SourceMeta {
        val categories = metaObject.optJsonArray("categories")?.mapNotNull { element ->
            val item = element.asJsonObject
            val name = item.optString("name")
            val typeId = item.optString("typeId")
            if (name.isBlank() || typeId.isBlank()) {
                null
            } else {
                Category(name = name, typeId = typeId)
            }
        }.orEmpty()
        return SourceMeta(
            title = metaObject.optString("title").ifBlank { "小猫影视" },
            host = metaObject.optString("host"),
            searchable = metaObject.optBoolean("searchable", true),
            categories = categories,
        )
    }

    private fun parseVideos(array: JsonArray?): List<VideoItem> {
        if (array == null) return emptyList()
        return array.mapNotNull { element ->
            val item = element.asJsonObject
            val detailUrl = item.optString("url")
            val title = item.optString("title")
            if (detailUrl.isBlank() || title.isBlank()) {
                null
            } else {
                VideoItem(
                    title = title,
                    coverUrl = item.optString("img"),
                    remarks = item.optString("desc"),
                    detailUrl = detailUrl,
                )
            }
        }
    }

    private fun parseDetail(detailObject: JsonObject): VideoDetail {
        val groups = detailObject.optJsonArray("playGroups")?.mapNotNull { groupElement ->
            val group = groupElement.asJsonObject
            val episodes = group.optJsonArray("episodes")?.mapNotNull { episodeElement ->
                val episode = episodeElement.asJsonObject
                val name = episode.optString("name")
                val url = episode.optString("url")
                if (name.isBlank() || url.isBlank()) {
                    null
                } else {
                    PlayEpisode(name = name, playUrl = url)
                }
            }.orEmpty()
            val groupName = group.optString("name")
            if (groupName.isBlank() || episodes.isEmpty()) {
                null
            } else {
                PlayGroup(name = groupName, episodes = episodes)
            }
        }.orEmpty()

        return VideoDetail(
            title = detailObject.optString("title"),
            coverUrl = detailObject.optString("img"),
            remarks = detailObject.optString("remarks"),
            year = detailObject.optString("year"),
            area = detailObject.optString("area"),
            director = detailObject.optString("director"),
            actor = detailObject.optString("actor"),
            summary = detailObject.optString("content"),
            playGroups = groups,
        )
    }

    private fun JsonObject.optString(name: String): String {
        return if (has(name) && !get(name).isJsonNull) get(name).asString else ""
    }

    private fun JsonObject.optBoolean(name: String, default: Boolean = false): Boolean {
        return if (has(name) && !get(name).isJsonNull) get(name).asBoolean else default
    }

    private fun JsonObject.optInt(name: String, default: Int = 0): Int {
        return if (has(name) && !get(name).isJsonNull) get(name).asInt else default
    }

    private fun JsonObject.optJsonObject(name: String): JsonObject? {
        return if (has(name) && get(name).isJsonObject) getAsJsonObject(name) else null
    }

    private fun JsonObject.optJsonArray(name: String): JsonArray? {
        return if (has(name) && get(name).isJsonArray) getAsJsonArray(name) else null
    }
}
