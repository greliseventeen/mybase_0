package com.example.konming_app.data.repository

import android.content.ContentValues
import android.database.Cursor
import com.example.konming_app.data.local.AppDatabaseHelper
import com.example.konming_app.data.model.BrowseHistoryItem

class BrowseHistoryRepository(private val dbHelper: AppDatabaseHelper) {

    fun addContentHistory(userId: Int, contentId: Int) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COLUMN_USER_ID, userId)
            put(AppDatabaseHelper.COLUMN_CONTENT_ID, contentId)
            put(AppDatabaseHelper.COLUMN_POST_ID, null as Int?)
            put(AppDatabaseHelper.COLUMN_BROWSE_TIME, System.currentTimeMillis())
        }
        db.insert(AppDatabaseHelper.TABLE_BROWSE_HISTORY, null, values)
        db.close()
    }

    fun addPostHistory(userId: Int, postId: Int) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COLUMN_USER_ID, userId)
            put(AppDatabaseHelper.COLUMN_CONTENT_ID, null as Int?)
            put(AppDatabaseHelper.COLUMN_POST_ID, postId)
            put(AppDatabaseHelper.COLUMN_BROWSE_TIME, System.currentTimeMillis())
        }
        db.insert(AppDatabaseHelper.TABLE_BROWSE_HISTORY, null, values)
        db.close()
    }

    fun getContentHistory(userId: Int, page: Int, pageSize: Int): List<BrowseHistoryItem.ContentHistory> {
        val list = mutableListOf<BrowseHistoryItem.ContentHistory>()
        val offset = (page - 1) * pageSize
        val db = dbHelper.readableDatabase

        val sql = """
            SELECT bh.id, bh.content_id, bh.browse_time, c.title, c.cover_path, c.category 
            FROM browse_history bh 
            INNER JOIN contents c ON bh.content_id = c.id 
            WHERE bh.user_id = ? AND bh.content_id IS NOT NULL 
            ORDER BY bh.browse_time DESC 
            LIMIT ? OFFSET ?
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf(userId.toString(), pageSize.toString(), offset.toString()))

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_ID))
                val contentId = it.getInt(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_CONTENT_ID))
                val title = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_TITLE))
                val coverPath = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_COVER_PATH))
                val category = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_CATEGORY))
                val browseTime = it.getLong(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_BROWSE_TIME))
                list.add(
                    BrowseHistoryItem.ContentHistory(id, contentId, title, coverPath, category, browseTime)
                )
            }
        }
        db.close()
        return list
    }

    fun getPostHistory(userId: Int, page: Int, pageSize: Int): List<BrowseHistoryItem.PostHistory> {
        val list = mutableListOf<BrowseHistoryItem.PostHistory>()
        val offset = (page - 1) * pageSize
        val db = dbHelper.readableDatabase

        val sql = """
            SELECT bh.id, bh.post_id, bh.browse_time, p.content, p.image_paths 
            FROM browse_history bh 
            INNER JOIN posts p ON bh.post_id = p.id 
            WHERE bh.user_id = ? AND bh.post_id IS NOT NULL 
            ORDER BY bh.browse_time DESC 
            LIMIT ? OFFSET ?
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf(userId.toString(), pageSize.toString(), offset.toString()))

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_ID))
                val postId = it.getInt(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_POST_ID))
                val content = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_CONTENT))
                val imagePaths = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_IMAGE_PATHS))
                val firstImagePath = if (imagePaths.isNullOrEmpty()) null else {
                    imagePaths.split(";", ",").firstOrNull()?.takeIf { it.isNotEmpty() }
                }
                val browseTime = it.getLong(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_BROWSE_TIME))
                list.add(
                    BrowseHistoryItem.PostHistory(id, postId, content, firstImagePath, browseTime)
                )
            }
        }
        db.close()
        return list
    }
}
