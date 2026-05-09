package com.example.konming_app.data.repository

import android.content.ContentValues
import android.database.Cursor
import com.example.konming_app.data.local.AppDatabaseHelper
import com.example.konming_app.data.local.PreferenceManager
import com.example.konming_app.data.model.User

class UserRepository(
    private val dbHelper: AppDatabaseHelper,
    private val preferenceManager: PreferenceManager? = null
) {

    fun register(username: String, password: String): Int? {
        if (checkUserExists(username)) {
            return null
        }

        val userId = generateUniqueUserId()
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COLUMN_ID, userId)
            put(AppDatabaseHelper.COLUMN_USERNAME, username)
            put(AppDatabaseHelper.COLUMN_PASSWORD, password)
            put(AppDatabaseHelper.COLUMN_NICKNAME, username)
        }

        val result = db.insert(AppDatabaseHelper.TABLE_USERS, null, values)
        return if (result != -1L) userId else null
    }

    private fun generateUniqueUserId(): Int {
        val random = kotlin.random.Random
        var userId: Int
        do {
            // 生成 100000 - 999999 的随机数
            userId = random.nextInt(900000) + 100000
        } while (checkUserIdExists(userId))
        return userId
    }

    private fun checkUserIdExists(userId: Int): Boolean {
        val db = dbHelper.readableDatabase
        val cursor: Cursor? = db.query(
            AppDatabaseHelper.TABLE_USERS,
            arrayOf(AppDatabaseHelper.COLUMN_ID),
            "${AppDatabaseHelper.COLUMN_ID} = ?",
            arrayOf(userId.toString()),
            null, null, null
        )

        val exists = cursor?.count ?: 0 > 0
        cursor?.close()
        return exists
    }

    fun login(username: String, password: String): Int? {
        val db = dbHelper.readableDatabase
        val cursor: Cursor? = db.query(
            AppDatabaseHelper.TABLE_USERS,
            arrayOf(AppDatabaseHelper.COLUMN_ID),
            "${AppDatabaseHelper.COLUMN_USERNAME} = ? AND ${AppDatabaseHelper.COLUMN_PASSWORD} = ?",
            arrayOf(username, password),
            null, null, null
        )

        var userId: Int? = null
        cursor?.use {
            if (it.moveToFirst()) {
                userId = it.getInt(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_ID))
            }
        }

        return userId
    }

    fun getUserInfo(userId: Int): User? {
        val db = dbHelper.readableDatabase
        val cursor: Cursor? = db.query(
            AppDatabaseHelper.TABLE_USERS,
            null,
            "${AppDatabaseHelper.COLUMN_ID} = ?",
            arrayOf(userId.toString()),
            null, null, null
        )

        var user: User? = null
        cursor?.use {
            if (it.moveToFirst()) {
                user = User(
                    id = it.getInt(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_ID)),
                    username = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_USERNAME)),
                    password = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_PASSWORD)),
                    nickname = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_NICKNAME)),
                    avatarPath = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_AVATAR_PATH)),
                    bio = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_BIO))
                )
            }
        }

        return user
    }

    private fun checkUserExists(username: String): Boolean {
        val db = dbHelper.readableDatabase
        val cursor: Cursor? = db.query(
            AppDatabaseHelper.TABLE_USERS,
            arrayOf(AppDatabaseHelper.COLUMN_ID),
            "${AppDatabaseHelper.COLUMN_USERNAME} = ?",
            arrayOf(username),
            null, null, null
        )

        val exists = cursor?.count ?: 0 > 0
        cursor?.close()
        return exists
    }

    fun updateAvatarPath(userId: Int, avatarPath: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COLUMN_AVATAR_PATH, avatarPath)
        }
        db.update(
            AppDatabaseHelper.TABLE_USERS,
            values,
            "${AppDatabaseHelper.COLUMN_ID} = ?",
            arrayOf(userId.toString())
        )
    }

    fun updateNickname(userId: Int, nickname: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COLUMN_NICKNAME, nickname)
        }
        db.update(
            AppDatabaseHelper.TABLE_USERS,
            values,
            "${AppDatabaseHelper.COLUMN_ID} = ?",
            arrayOf(userId.toString())
        )
    }

    fun updateBio(userId: Int, bio: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COLUMN_BIO, bio)
        }
        db.update(
            AppDatabaseHelper.TABLE_USERS,
            values,
            "${AppDatabaseHelper.COLUMN_ID} = ?",
            arrayOf(userId.toString())
        )
    }

    fun updatePassword(userId: Int, password: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COLUMN_PASSWORD, password)
        }
        db.update(
            AppDatabaseHelper.TABLE_USERS,
            values,
            "${AppDatabaseHelper.COLUMN_ID} = ?",
            arrayOf(userId.toString())
        )
    }
}