package com.xiaomao.shell.data.model

data class Category(
    val name: String,
    val typeId: String,
)

data class SourceMeta(
    val title: String,
    val host: String,
    val searchable: Boolean,
    val categories: List<Category>,
)

data class VideoItem(
    val title: String,
    val coverUrl: String,
    val remarks: String,
    val detailUrl: String,
)

data class PlayEpisode(
    val name: String,
    val playUrl: String,
)

data class PlayGroup(
    val name: String,
    val episodes: List<PlayEpisode>,
)

data class VideoDetail(
    val title: String,
    val coverUrl: String,
    val remarks: String,
    val year: String,
    val area: String,
    val director: String,
    val actor: String,
    val summary: String,
    val playGroups: List<PlayGroup>,
)

data class PlayResult(
    val url: String,
    val parse: Boolean,
    val jx: Int,
    val headers: Map<String, String>,
)

data class HomePage(
    val meta: SourceMeta,
    val videos: List<VideoItem>,
)

