package com.example.konming_app.ui.upload

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.konming_app.data.local.PreferenceManager
import com.example.konming_app.data.model.Content
import com.example.konming_app.data.repository.ContentRepository
import com.example.konming_app.data.repository.RepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UploadContentViewModel : ViewModel() {
    private val contentRepository: ContentRepository by lazy {
        RepositoryFactory.getContentRepository()
    }

    private val preferenceManager: PreferenceManager by lazy {
        RepositoryFactory.getPreferenceManager()
    }

    // 上传状态
    private val _isUploading = MutableLiveData<Boolean>()
    val isUploading: LiveData<Boolean> = _isUploading

    // 上传结果
    private val _uploadResult = MutableLiveData<Boolean>()
    val uploadResult: LiveData<Boolean> = _uploadResult

    // 错误信息
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    /**
     * 发布内容
     */
    fun publishContent(
        title: String,
        category: String,
        description: String,
        filePath: String,
        coverPath: String,
        fileType: String
    ) {
        _isUploading.value = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = preferenceManager.getLoggedInUserId()
                val currentTime = System.currentTimeMillis()

                val content = Content(
                    id = 0,
                    userId = userId,
                    title = title,
                    coverPath = coverPath,
                    category = category,
                    desc = description,
                    filePath = filePath,
                    type = fileType,
                    duration = 0,
                    timestamp = currentTime
                )

                val resultId = contentRepository.insertContent(content)

                withContext(Dispatchers.Main) {
                    _uploadResult.value = (resultId > 0)
                    _isUploading.value = false

                    if (resultId <= 0) {
                        _errorMessage.value = "上传失败，请重试"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uploadResult.value = false
                    _isUploading.value = false
                    _errorMessage.value = "上传失败，请重试"
                }
            }
        }
    }

    /**
     * 重置上传状态
     */
    fun resetUploadState() {
        _uploadResult.value = null
        _errorMessage.value = null
    }
}
