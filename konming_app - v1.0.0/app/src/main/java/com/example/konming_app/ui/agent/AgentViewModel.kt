package com.example.konming_app.ui.agent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AgentViewModel : ViewModel() {
    private val _statusText = MutableLiveData<String>()
    val statusText: LiveData<String> = _statusText

    init {
        _statusText.value = "AgentViewModel ready"
    }
}