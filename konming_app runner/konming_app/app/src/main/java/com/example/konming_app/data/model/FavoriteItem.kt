package com.example.konming_app.data.model

sealed class FavoriteItem {
    data class ContentItem(
        val contentId: Int,
        val title: String,
        val coverPath: String?,
        val category: String?,
        val timestamp: Long
    ) : FavoriteItem()

    data class PostItem(
        val postId: Int,
        val content: String,
        val firstImagePath: String?,
        val timestamp: Long
    ) : FavoriteItem()
}
