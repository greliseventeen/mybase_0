package com.example.konming_app.data.repository

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.konming_app.data.local.AppDatabaseHelper
import com.example.konming_app.data.model.Content
import java.io.File

class ContentRepository(private val dbHelper: AppDatabaseHelper) {

    fun insertContent(content: Content): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COLUMN_USER_ID, content.userId)
            put(AppDatabaseHelper.COLUMN_TITLE, content.title)
            put(AppDatabaseHelper.COLUMN_COVER_PATH, content.coverPath)
            put(AppDatabaseHelper.COLUMN_CATEGORY, content.category)
            put(AppDatabaseHelper.COLUMN_DESCRIPTION, content.desc)
            put(AppDatabaseHelper.COLUMN_FILE_PATH, content.filePath)
            put(AppDatabaseHelper.COLUMN_TYPE, content.type)
            put(AppDatabaseHelper.COLUMN_DURATION, content.duration)
            put(AppDatabaseHelper.COLUMN_TIMESTAMP, content.timestamp)
        }
        return db.insert(AppDatabaseHelper.TABLE_CONTENTS, null, values)
    }

    fun getAllContents(page: Int, pageSize: Int): List<Content> {
        val db = dbHelper.readableDatabase
        val offset = (page - 1) * pageSize
        val cursor: Cursor? = db.query(
            AppDatabaseHelper.TABLE_CONTENTS,
            null,
            null,
            null,
            null, null,
            "${AppDatabaseHelper.COLUMN_TIMESTAMP} DESC",
            "$pageSize OFFSET $offset"
        )

        val contents = mutableListOf<Content>()
        cursor?.use {
            while (it.moveToNext()) {
                contents.add(cursorToContent(it))
            }
        }
        return contents
    }

    fun getContentById(id: Int): Content? {
        val db = dbHelper.readableDatabase
        val cursor: Cursor? = db.query(
            AppDatabaseHelper.TABLE_CONTENTS,
            null,
            "${AppDatabaseHelper.COLUMN_ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        )

        var content: Content? = null
        cursor?.use {
            if (it.moveToFirst()) {
                content = cursorToContent(it)
            }
        }
        return content
    }

    fun addToFavorites(userId: Int, contentId: Int): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COLUMN_USER_ID, userId)
            put(AppDatabaseHelper.COLUMN_CONTENT_ID, contentId)
            put(AppDatabaseHelper.COLUMN_TIMESTAMP, System.currentTimeMillis())
        }
        val result = db.insertWithOnConflict(
            AppDatabaseHelper.TABLE_FAVORITES,
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        )
        return result != -1L
    }

    fun removeFavorite(userId: Int, contentId: Int): Boolean {
        val db = dbHelper.writableDatabase
        val rowsDeleted = db.delete(
            AppDatabaseHelper.TABLE_FAVORITES,
            "${AppDatabaseHelper.COLUMN_USER_ID} = ? AND ${AppDatabaseHelper.COLUMN_CONTENT_ID} = ?",
            arrayOf(userId.toString(), contentId.toString())
        )
        return rowsDeleted > 0
    }

    fun isFavorited(userId: Int, contentId: Int): Boolean {
        val db = dbHelper.readableDatabase
        val cursor: Cursor? = db.query(
            AppDatabaseHelper.TABLE_FAVORITES,
            null,
            "${AppDatabaseHelper.COLUMN_USER_ID} = ? AND ${AppDatabaseHelper.COLUMN_CONTENT_ID} = ?",
            arrayOf(userId.toString(), contentId.toString()),
            null, null, null
        )
        val result = cursor?.count ?: 0 > 0
        cursor?.close()
        return result
    }

    fun getFavoriteContentIds(userId: Int): List<Int> {
        val db = dbHelper.readableDatabase
        val cursor: Cursor? = db.query(
            AppDatabaseHelper.TABLE_FAVORITES,
            arrayOf(AppDatabaseHelper.COLUMN_CONTENT_ID),
            "${AppDatabaseHelper.COLUMN_USER_ID} = ? AND ${AppDatabaseHelper.COLUMN_CONTENT_ID} IS NOT NULL",
            arrayOf(userId.toString()),
            null, null, null
        )

        val ids = mutableListOf<Int>()
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_CONTENT_ID))
                ids.add(id)
            }
        }
        return ids
    }

    private fun cursorToContent(cursor: Cursor): Content {
        return Content(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_ID)),
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_USER_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_TITLE)),
            coverPath = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_COVER_PATH)),
            category = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_CATEGORY)),
            desc = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_DESCRIPTION)),
            filePath = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_FILE_PATH)),
            type = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_TYPE)),
            duration = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_DURATION)),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_TIMESTAMP))
        )
    }

    fun getContentsByUserId(userId: Int): List<Content> {
        val db = dbHelper.readableDatabase
        val cursor: Cursor? = db.query(
            AppDatabaseHelper.TABLE_CONTENTS,
            null,
            "${AppDatabaseHelper.COLUMN_USER_ID} = ?",
            arrayOf(userId.toString()),
            null, null,
            "${AppDatabaseHelper.COLUMN_TIMESTAMP} DESC"
        )

        val contents = mutableListOf<Content>()
        cursor?.use {
            while (it.moveToNext()) {
                contents.add(cursorToContent(it))
            }
        }
        return contents
    }

    fun getFavoriteContents(userId: Int): List<Content> {
        val db = dbHelper.readableDatabase
        val query = """
            SELECT c.* 
            FROM ${AppDatabaseHelper.TABLE_FAVORITES} f
            INNER JOIN ${AppDatabaseHelper.TABLE_CONTENTS} c ON f.${AppDatabaseHelper.COLUMN_CONTENT_ID} = c.${AppDatabaseHelper.COLUMN_ID}
            WHERE f.${AppDatabaseHelper.COLUMN_USER_ID} = ? AND f.${AppDatabaseHelper.COLUMN_CONTENT_ID} IS NOT NULL
            ORDER BY f.${AppDatabaseHelper.COLUMN_TIMESTAMP} DESC
        """
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        val contents = mutableListOf<Content>()
        cursor.use {
            while (it.moveToNext()) {
                contents.add(cursorToContent(it))
            }
        }
        return contents
    }

    fun deleteContent(contentId: Int) {
        val content = getContentById(contentId)
        content?.let {
            // 删除文件
            it.coverPath?.let { path ->
                val coverFile = File(path)
                if (coverFile.exists()) {
                    coverFile.delete()
                }
            }
            it.filePath?.let { path ->
                val mediaFile = File(path)
                if (mediaFile.exists()) {
                    mediaFile.delete()
                }
            }
        }
        val db = dbHelper.writableDatabase
        db.delete(
            AppDatabaseHelper.TABLE_FAVORITES,
            "${AppDatabaseHelper.COLUMN_CONTENT_ID} = ?",
            arrayOf(contentId.toString())
        )
        db.delete(
            AppDatabaseHelper.TABLE_CONTENTS,
            "${AppDatabaseHelper.COLUMN_ID} = ?",
            arrayOf(contentId.toString())
        )
    }
}