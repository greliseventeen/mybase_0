package com.example.konming_app.data.model

data class Post(
    val id: Int = 0,
    val userId: Int,
    val content: String,
    val imagePaths: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)