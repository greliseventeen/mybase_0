package com.example.konming_app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.konming_app.R
import com.example.konming_app.data.model.Content
import com.example.konming_app.data.model.User
import com.example.konming_app.data.repository.ContentRepository
import com.example.konming_app.data.repository.RepositoryFactory
import com.example.konming_app.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val contentRepository: ContentRepository = RepositoryFactory.getContentRepository()
    private val userRepository: UserRepository = RepositoryFactory.getUserRepository()
    
    private val _allContents = MutableLiveData<List<Content>>(emptyList())
    val allContents: LiveData<List<Content>> get() = _allContents
    
    private val _filteredContents = MutableLiveData<List<Content>>(emptyList())
    val filteredContents: LiveData<List<Content>> get() = _filteredContents
    
    private val _userCache = MutableLiveData<Map<Int, User>>(emptyMap())
    val userCache: LiveData<Map<Int, User>> get() = _userCache
    
    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> get() = _categories
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading
    
    private val _isRefreshing = MutableLiveData<Boolean>(false)
    val isRefreshing: LiveData<Boolean> get() = _isRefreshing
    
    private val _hasMoreData = MutableLiveData<Boolean>(true)
    val hasMoreData: LiveData<Boolean> get() = _hasMoreData
    
    private var currentPage = 1
    private val pageSize = 10
    private var currentCategory = ""
    
    init {
        loadCategories()
        refresh()
    }
    
    private fun loadCategories() {
        val categoryList = getApplication<Application>().resources.getStringArray(R.array.content_categories).toMutableList()
        categoryList.add(0, "全部")
        _categories.value = categoryList
    }
    
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _isLoading.value = true
            currentPage = 1
            _hasMoreData.value = true
            
            withContext(Dispatchers.IO) {
                val newContents = contentRepository.getAllContents(currentPage, pageSize)
                _allContents.postValue(newContents)
                _hasMoreData.postValue(newContents.size == pageSize)
                loadUserCache(newContents)
            }
            
            filterByCategory(currentCategory)
            _isRefreshing.value = false
            _isLoading.value = false
        }
    }
    
    fun loadMore() {
        if ((_isLoading.value ?: false) || (_hasMoreData.value ?: true).not()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            
            withContext(Dispatchers.IO) {
                currentPage++
                val newContents = contentRepository.getAllContents(currentPage, pageSize)
                val currentList = _allContents.value.orEmpty()
                val updatedList = currentList + newContents
                _allContents.postValue(updatedList)
                _hasMoreData.postValue(newContents.size == pageSize)
                loadUserCache(updatedList)
            }
            
            filterByCategory(currentCategory)
            _isLoading.value = false
        }
    }
    
    private fun loadUserCache(contents: List<Content>) {
        val userIds = contents.map { it.userId }.distinct()
        val currentCache = _userCache.value.orEmpty().toMutableMap()
        userIds.forEach { userId ->
            if (!currentCache.containsKey(userId)) {
                val user = userRepository.getUserInfo(userId)
                if (user != null) {
                    currentCache[userId] = user
                }
            }
        }
        _userCache.postValue(currentCache)
    }
    
    fun filterByCategory(category: String) {
        currentCategory = category
        val allList = _allContents.value ?: emptyList()
        
        if (category.isEmpty() || category == "全部") {
            _filteredContents.value = allList
        } else {
            _filteredContents.value = allList.filter { it.category == category }
        }
    }
}
