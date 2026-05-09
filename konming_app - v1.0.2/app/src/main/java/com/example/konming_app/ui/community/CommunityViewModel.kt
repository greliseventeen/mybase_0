package com.example.konming_app.ui.community

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.konming_app.data.model.Post
import com.example.konming_app.data.repository.PostRepository
import com.example.konming_app.data.repository.RepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommunityViewModel : ViewModel() {
    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private var currentPage = 1
    private val pageSize = 10
    private val postRepository: PostRepository by lazy {
        RepositoryFactory.getPostRepository()
    }

    init {
        loadPosts(1)
    }

    fun loadPosts(page: Int) {
        if (page == 1) {
            _isRefreshing.value = true
        } else {
            _isLoading.value = true
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val postList = postRepository.getAllPosts(page, pageSize)
                withContext(Dispatchers.Main) {
                    if (page == 1) {
                        _posts.value = postList
                    } else {
                        val currentList = _posts.value.orEmpty().toMutableList()
                        currentList.addAll(postList)
                        _posts.value = currentList
                    }
                    currentPage = page
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                    withContext(Dispatchers.Main) {
                        _isRefreshing.value = false
                        _isLoading.value = false
                    }
            }
        }
    }

    fun refresh() {
        loadPosts(1)
    }

    fun loadMore() {
        loadPosts(currentPage + 1)
    }
}
