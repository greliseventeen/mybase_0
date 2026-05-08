package com.example.konming_app.data.repository

import android.content.ContentValues
import android.database.Cursor
import com.example.konming_app.data.local.AppDatabaseHelper
import com.example.konming_app.data.model.Post
import java.io.File

class PostRepository(private val dbHelper: AppDatabaseHelper) {

    fun insertPost(post: Post): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COLUMN_USER_ID, post.userId)
            put(AppDatabaseHelper.COLUMN_CONTENT, post.content)
            put(AppDatabaseHelper.COLUMN_IMAGE_PATHS, post.imagePaths)
            put(AppDatabaseHelper.COLUMN_TIMESTAMP, post.timestamp)
        }
        return db.insert(AppDatabaseHelper.TABLE_POSTS, null, values)
    }

    fun getAllPosts(page: Int, pageSize: Int): List<Post> {
        val db = dbHelper.readableDatabase
        val offset = (page - 1) * pageSize
        val cursor: Cursor? = db.query(
            AppDatabaseHelper.TABLE_POSTS,
            null,
            null,
            null,
            null, null,
            "${AppDatabaseHelper.COLUMN_TIMESTAMP} DESC",
            "$pageSize OFFSET $offset"
        )

        val posts = mutableListOf<Post>()
        cursor?.use {
            while (it.moveToNext()) {
                posts.add(cursorToPost(it))
            }
        }
        return posts
    }

    fun getPostById(id: Int): Post? {
        val db = dbHelper.readableDatabase
        val cursor: Cursor? = db.query(
            AppDatabaseHelper.TABLE_POSTS,
            null,
            "${AppDatabaseHelper.COLUMN_ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        )

        var post: Post? = null
        cursor?.use {
            if (it.moveToFirst()) {
                post = cursorToPost(it)
            }
        }
        return post
    }

    private fun cursorToPost(cursor: Cursor): Post {
        return Post(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_ID)),
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_USER_ID)),
            content = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_CONTENT)),
            imagePaths = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_IMAGE_PATHS)),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_TIMESTAMP))
        )
    }

    fun addToFavorites(userId: Int, postId: Int): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COLUMN_USER_ID, userId)
            put(AppDatabaseHelper.COLUMN_POST_ID, postId)
            put(AppDatabaseHelper.COLUMN_TIMESTAMP, System.currentTimeMillis())
        }
        val result = db.insertWithOnConflict(
            AppDatabaseHelper.TABLE_FAVORITES,
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
        )
        return result != -1L
    }

    fun removeFavorite(userId: Int, postId: Int): Boolean {
        val db = dbHelper.writableDatabase
        val rowsDeleted = db.delete(
            AppDatabaseHelper.TABLE_FAVORITES,
            "${AppDatabaseHelper.COLUMN_USER_ID} = ? AND ${AppDatabaseHelper.COLUMN_POST_ID} = ?",
            arrayOf(userId.toString(), postId.toString())
        )
        return rowsDeleted > 0
    }

    fun isFavorited(userId: Int, postId: Int): Boolean {
        val db = dbHelper.readableDatabase
        val cursor: android.database.Cursor? = db.query(
            AppDatabaseHelper.TABLE_FAVORITES,
            null,
            "${AppDatabaseHelper.COLUMN_USER_ID} = ? AND ${AppDatabaseHelper.COLUMN_POST_ID} = ?",
            arrayOf(userId.toString(), postId.toString()),
            null, null, null
        )
        val result = cursor?.count ?: 0 > 0
        cursor?.close()
        return result
    }

    fun getFavoritePostIds(userId: Int): List<Int> {
        val db = dbHelper.readableDatabase
        val cursor: android.database.Cursor? = db.query(
            AppDatabaseHelper.TABLE_FAVORITES,
            arrayOf(AppDatabaseHelper.COLUMN_POST_ID),
            "${AppDatabaseHelper.COLUMN_USER_ID} = ? AND ${AppDatabaseHelper.COLUMN_POST_ID} IS NOT NULL",
            arrayOf(userId.toString()),
            null, null, null
        )

        val ids = mutableListOf<Int>()
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_POST_ID))
                ids.add(id)
            }
        }
        return ids
    }

    fun getPostsByUserId(userId: Int): List<Post> {
        val db = dbHelper.readableDatabase
        val cursor: android.database.Cursor? = db.query(
            AppDatabaseHelper.TABLE_POSTS,
            null,
            "${AppDatabaseHelper.COLUMN_USER_ID} = ?",
            arrayOf(userId.toString()),
            null, null,
            "${AppDatabaseHelper.COLUMN_TIMESTAMP} DESC"
        )

        val posts = mutableListOf<Post>()
        cursor?.use {
            while (it.moveToNext()) {
                posts.add(cursorToPost(it))
            }
        }
        return posts
    }

    fun getFavoritePosts(userId: Int): List<Post> {
        val db = dbHelper.readableDatabase
        val query = """
            SELECT p.* 
            FROM ${AppDatabaseHelper.TABLE_FAVORITES} f
            INNER JOIN ${AppDatabaseHelper.TABLE_POSTS} p ON f.${AppDatabaseHelper.COLUMN_POST_ID} = p.${AppDatabaseHelper.COLUMN_ID}
            WHERE f.${AppDatabaseHelper.COLUMN_USER_ID} = ? AND f.${AppDatabaseHelper.COLUMN_POST_ID} IS NOT NULL
            ORDER BY f.${AppDatabaseHelper.COLUMN_TIMESTAMP} DESC
        """
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        val posts = mutableListOf<Post>()
        cursor.use {
            while (it.moveToNext()) {
                posts.add(cursorToPost(it))
            }
        }
        return posts
    }

    fun deletePost(postId: Int) {
        val post = getPostById(postId)
        post?.let {
            // 删除图片文件
            it.imagePaths?.let { paths ->
                paths.split(",").forEach { path ->
                    val imageFile = File(path)
                    if (imageFile.exists()) {
                        imageFile.delete()
                    }
                }
            }
        }
        val db = dbHelper.writableDatabase
        db.delete(
            AppDatabaseHelper.TABLE_FAVORITES,
            "${AppDatabaseHelper.COLUMN_POST_ID} = ?",
            arrayOf(postId.toString())
        )
        db.delete(
            AppDatabaseHelper.TABLE_POSTS,
            "${AppDatabaseHelper.COLUMN_ID} = ?",
            arrayOf(postId.toString())
        )
    }
}