package com.example.konming_app.ui.mine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.konming_app.data.local.PreferenceManager
import com.example.konming_app.data.model.User
import com.example.konming_app.data.repository.RepositoryFactory
import com.example.konming_app.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MineViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = RepositoryFactory.getUserRepository()
    private val preferenceManager = RepositoryFactory.getPreferenceManager()

    private val _userInfo = MutableLiveData<User?>()
    val userInfo: LiveData<User?> = _userInfo

    private val _ipLocation = MutableLiveData<String>()
    val ipLocation: LiveData<String> = _ipLocation

    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

    var userId: Int = -1
        private set

    private val provinces = listOf(
        "北京", "上海", "广东", "浙江", "江苏", "四川", "湖北", "山东",
        "湖南", "河南", "河北", "福建", "安徽", "陕西", "江西", "重庆"
    )

    init {
        userId = preferenceManager.getLoggedInUserId()
        refreshUserInfo()
        refreshIpLocation()
    }

    private fun getSimulatedLocation(): String {
        val savedLocation = preferenceManager.getUserIpLocation()
        if (!savedLocation.isNullOrEmpty()) {
            return savedLocation
        }
        val random = kotlin.random.Random
        val location = provinces.random(random)
        preferenceManager.saveUserIpLocation(location)
        return location
    }

    fun refreshIpLocation() {
        val location = getSimulatedLocation()
        _ipLocation.value = location
    }

    fun regenerateIpLocation() {
        val random = kotlin.random.Random
        val location = provinces.random(random)
        preferenceManager.saveUserIpLocation(location)
        _ipLocation.value = location
    }

    fun refreshUserInfo() {
        if (userId == -1) {
            return
        }

        viewModelScope.launch {
            val user = withContext(Dispatchers.IO) {
                userRepository.getUserInfo(userId)
            }
            _userInfo.value = user
        }
    }

    fun updateNickname(nickname: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                userRepository.updateNickname(userId, nickname)
            }
            refreshUserInfo()
        }
    }

    fun updateBio(bio: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                userRepository.updateBio(userId, bio)
            }
            refreshUserInfo()
        }
    }

    fun logout() {
        preferenceManager.clearLoginState()
        _logoutEvent.value = true
    }

    fun onLogoutEventConsumed() {
        _logoutEvent.value = false
    }

    fun updateAvatarPath(avatarPath: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                userRepository.updateAvatarPath(userId, avatarPath)
            }
            refreshUserInfo()
        }
    }
}
