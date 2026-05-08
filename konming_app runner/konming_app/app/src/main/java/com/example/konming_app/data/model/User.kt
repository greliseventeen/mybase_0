package com.example.konming_app.data.model

data class User(
    val id: Int = 0,
    val username: String,
    val password: String = "",
    val nickname: String? = null,
    val avatarPath: String? = null,
    val bio: String? = null
)