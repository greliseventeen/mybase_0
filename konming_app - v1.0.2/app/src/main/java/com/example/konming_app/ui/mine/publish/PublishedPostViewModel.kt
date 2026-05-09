package com.example.konming_app.ui.mine.publish

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.konming_app.data.local.PreferenceManager
import com.example.konming_app.data.model.Post
import com.example.konming_app.data.repository.PostRepository
import com.example.konming_app.data.repository.RepositoryFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PublishedPostViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = RepositoryFactory.getPostRepository()
    private val prefManager = RepositoryFactory.getPreferenceManager()
    private val userId = prefManager.getLoggedInUserId()

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadPosts() {
        if (userId == -1) return
        viewModelScope.launch {
            _isLoading.value = true
            val list = withContext(Dispatchers.IO) {
                repo.getPostsByUserId(userId)
            }
            _posts.value = list
            _isLoading.value = false
        }
    }

    fun deletePost(postId: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.deletePost(postId)
            }
            loadPosts()
        }
    }
}
