package com.example.konming_app.ui.browse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.konming_app.data.model.BrowseHistoryItem
import com.example.konming_app.data.repository.RepositoryFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BrowsedPostHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = RepositoryFactory.getBrowseHistoryRepository()
    private val prefManager = RepositoryFactory.getPreferenceManager()

    private val _items = MutableLiveData<List<BrowseHistoryItem.PostHistory>>()
    val items: LiveData<List<BrowseHistoryItem.PostHistory>> = _items

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var currentPage = 1
    private val pageSize = 20
    private var hasMore = true

    fun loadData(refresh: Boolean = false) {
        if (refresh) {
            currentPage = 1
            hasMore = true
        }
        val userId = prefManager.getLoggedInUserId()
        if (userId == -1) return

        viewModelScope.launch {
            _isLoading.value = true
            val list = withContext(Dispatchers.IO) {
                repo.getPostHistory(userId, currentPage, pageSize)
            }
            if (refresh) {
                _items.value = list
            } else {
                _items.value = _items.value.orEmpty() + list
            }
            hasMore = list.size >= pageSize
            currentPage++
            _isLoading.value = false
        }
    }
}
