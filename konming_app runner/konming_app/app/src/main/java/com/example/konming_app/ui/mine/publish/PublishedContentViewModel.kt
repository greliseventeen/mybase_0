package com.example.konming_app.ui.mine.publish

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.konming_app.data.local.PreferenceManager
import com.example.konming_app.data.model.Content
import com.example.konming_app.data.repository.ContentRepository
import com.example.konming_app.data.repository.RepositoryFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PublishedContentViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = RepositoryFactory.getContentRepository()
    private val prefManager = RepositoryFactory.getPreferenceManager()
    private val userId = prefManager.getLoggedInUserId()

    private val _contents = MutableLiveData<List<Content>>()
    val contents: LiveData<List<Content>> = _contents

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadContents() {
        if (userId == -1) return
        viewModelScope.launch {
            _isLoading.value = true
            val list = withContext(Dispatchers.IO) {
                repo.getContentsByUserId(userId)
            }
            _contents.value = list
            _isLoading.value = false
        }
    }

    fun deleteContent(contentId: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.deleteContent(contentId)
            }
            loadContents()
        }
    }
}
