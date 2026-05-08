package com.example.konming_app.data.model

sealed class BrowseHistoryItem {
    abstract val id: Int
    abstract val browseTime: Long

    data class ContentHistory(
        override val id: Int,
        val contentId: Int,
        val title: String,
        val coverPath: String?,
        val category: String?,
        override val browseTime: Long
    ) : BrowseHistoryItem()

    data class PostHistory(
        override val id: Int,
        val postId: Int,
        val content: String,
        val firstImagePath: String?,
        override val browseTime: Long
    ) : BrowseHistoryItem()
}
