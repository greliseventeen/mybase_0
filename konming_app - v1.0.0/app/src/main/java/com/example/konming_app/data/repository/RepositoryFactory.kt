package com.example.konming_app.data.repository

import android.content.Context
import com.example.konming_app.data.local.AppDatabaseHelper
import com.example.konming_app.data.local.PreferenceManager

object RepositoryFactory {
    private var appDatabaseHelper: AppDatabaseHelper? = null
    private var preferenceManager: PreferenceManager? = null

    private var userRepository: UserRepository? = null
    private var contentRepository: ContentRepository? = null
    private var postRepository: PostRepository? = null
    private var browseHistoryRepository: BrowseHistoryRepository? = null

    fun initialize(context: Context) {
        if (appDatabaseHelper == null) {
            appDatabaseHelper = AppDatabaseHelper(context.applicationContext)
            preferenceManager = PreferenceManager(context.applicationContext)
        }
    }

    fun getUserRepository(): UserRepository {
        return userRepository ?: UserRepository(
            requireNotNull(appDatabaseHelper),
            requireNotNull(preferenceManager)
        ).also { userRepository = it }
    }

    fun getContentRepository(): ContentRepository {
        return contentRepository ?: ContentRepository(requireNotNull(appDatabaseHelper))
            .also { contentRepository = it }
    }

    fun getPostRepository(): PostRepository {
        return postRepository ?: PostRepository(requireNotNull(appDatabaseHelper))
            .also { postRepository = it }
    }

    fun getBrowseHistoryRepository(): BrowseHistoryRepository {
        return browseHistoryRepository ?: BrowseHistoryRepository(requireNotNull(appDatabaseHelper))
            .also { browseHistoryRepository = it }
    }

    fun getPreferenceManager(): PreferenceManager {
        return requireNotNull(preferenceManager)
    }
}