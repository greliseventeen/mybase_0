package com.example.konming_app.data.model

data class Content(
    val id: Int = 0,
    val userId: Int,
    val title: String,
    val coverPath: String? = null,
    val category: String? = null,
    val desc: String? = null,
    val filePath: String? = null,
    val type: String = "article",
    val duration: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isLandscape: Boolean = true
)