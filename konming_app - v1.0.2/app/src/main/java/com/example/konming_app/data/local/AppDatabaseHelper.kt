package com.example.konming_app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "konming.db"
        private const val DATABASE_VERSION = 3

        const val TABLE_USERS = "users"
        const val TABLE_CONTENTS = "contents"
        const val TABLE_POSTS = "posts"
        const val TABLE_FAVORITES = "favorites"
        const val TABLE_BROWSE_HISTORY = "browse_history"

        const val COLUMN_BROWSE_TIME = "browse_time"

        const val COLUMN_ID = "id"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_PASSWORD = "password"
        const val COLUMN_NICKNAME = "nickname"
        const val COLUMN_AVATAR_PATH = "avatar_path"
        const val COLUMN_BIO = "bio"

        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_COVER_PATH = "cover_path"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_FILE_PATH = "file_path"
        const val COLUMN_TYPE = "type"
        const val COLUMN_DURATION = "duration"
        const val COLUMN_TIMESTAMP = "timestamp"

        const val COLUMN_CONTENT = "content"
        const val COLUMN_IMAGE_PATHS = "image_paths"

        const val COLUMN_CONTENT_ID = "content_id"
        const val COLUMN_POST_ID = "post_id"

        private const val CREATE_TABLE_USERS = """
            CREATE TABLE IF NOT EXISTS $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY,
                $COLUMN_USERNAME TEXT UNIQUE NOT NULL,
                $COLUMN_PASSWORD TEXT NOT NULL,
                $COLUMN_NICKNAME TEXT,
                $COLUMN_AVATAR_PATH TEXT,
                $COLUMN_BIO TEXT
            )
        """

        private const val CREATE_TABLE_CONTENTS = """
            CREATE TABLE IF NOT EXISTS $TABLE_CONTENTS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID INTEGER NOT NULL,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_COVER_PATH TEXT,
                $COLUMN_CATEGORY TEXT,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_FILE_PATH TEXT,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_DURATION INTEGER,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            )
        """

        private const val CREATE_TABLE_POSTS = """
            CREATE TABLE IF NOT EXISTS $TABLE_POSTS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID INTEGER NOT NULL,
                $COLUMN_CONTENT TEXT NOT NULL,
                $COLUMN_IMAGE_PATHS TEXT,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            )
        """

        private const val CREATE_TABLE_FAVORITES = """
            CREATE TABLE IF NOT EXISTS $TABLE_FAVORITES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID INTEGER NOT NULL,
                $COLUMN_CONTENT_ID INTEGER,
                $COLUMN_POST_ID INTEGER,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                UNIQUE($COLUMN_USER_ID, $COLUMN_CONTENT_ID, $COLUMN_POST_ID)
            )
        """

        private const val CREATE_TABLE_BROWSE_HISTORY = """
            CREATE TABLE IF NOT EXISTS $TABLE_BROWSE_HISTORY (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID INTEGER NOT NULL,
                $COLUMN_CONTENT_ID INTEGER,
                $COLUMN_POST_ID INTEGER,
                $COLUMN_BROWSE_TIME INTEGER NOT NULL
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_USERS)
        db.execSQL(CREATE_TABLE_CONTENTS)
        db.execSQL(CREATE_TABLE_POSTS)
        db.execSQL(CREATE_TABLE_FAVORITES)
        db.execSQL(CREATE_TABLE_BROWSE_HISTORY)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(CREATE_TABLE_BROWSE_HISTORY)
        }
        if (oldVersion < 3) {
            // 重建 users 表，去掉 AUTOINCREMENT
            db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
            db.execSQL(CREATE_TABLE_USERS)
        }
    }

    override fun getReadableDatabase(): SQLiteDatabase {
        return super.getReadableDatabase()
    }

    override fun getWritableDatabase(): SQLiteDatabase {
        return super.getWritableDatabase()
    }
}