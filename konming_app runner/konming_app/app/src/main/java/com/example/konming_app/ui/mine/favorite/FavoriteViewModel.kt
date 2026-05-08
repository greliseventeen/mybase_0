package com.example.konming_app.ui.mine.favorite

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.konming_app.data.local.PreferenceManager
import com.example.konming_app.data.model.FavoriteItem
import com.example.konming_app.data.repository.ContentRepository
import com.example.konming_app.data.repository.PostRepository
import com.example.konming_app.data.repository.RepositoryFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteViewModel(application: Application) : AndroidViewModel(application) {
    private val contentRepo = RepositoryFactory.getContentRepository()
    private val postRepo = RepositoryFactory.getPostRepository()
    private val prefManager = RepositoryFactory.getPreferenceManager()
    private val userId = prefManager.getLoggedInUserId()

    private val _favorites = MutableLiveData<List<FavoriteItem>>()
    val favorites: LiveData<List<FavoriteItem>> = _favorites

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadFavorites() {
        if (userId == -1) return
        viewModelScope.launch {
            _isLoading.value = true
            val items = withContext(Dispatchers.IO) {
                val contentList = contentRepo.getFavoriteContents(userId)
                val postList = postRepo.getFavoritePosts(userId)

                val result = mutableListOf<FavoriteItem>()
                contentList.forEach { content ->
                    result.add(
                        FavoriteItem.ContentItem(
                            contentId = content.id,
                            title = content.title,
                            coverPath = content.coverPath,
                            category = content.category,
                            timestamp = content.timestamp
                        )
                    )
                }
                postList.forEach { post ->
                    val firstImagePath = post.imagePaths?.split(",")?.firstOrNull()
                    result.add(
                        FavoriteItem.PostItem(
                            postId = post.id,
                            content = post.content,
                            firstImagePath = firstImagePath,
                            timestamp = post.timestamp
                        )
                    )
                }
                result.sortedByDescending { item ->
                    when (item) {
                        is FavoriteItem.ContentItem -> item.timestamp
                        is FavoriteItem.PostItem -> item.timestamp
                    }
                }
            }
            _favorites.value = items
            _isLoading.value = false
        }
    }

    fun removeFavorite(item: FavoriteItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                when (item) {
                    is FavoriteItem.ContentItem -> {
                        contentRepo.removeFavorite(userId, item.contentId)
                    }
                    is FavoriteItem.PostItem -> {
                        postRepo.removeFavorite(userId, item.postId)
                    }
                }
            }
            loadFavorites()
        }
    }
}
