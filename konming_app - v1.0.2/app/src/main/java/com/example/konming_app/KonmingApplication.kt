package com.example.konming_app

import android.app.Application
import com.example.konming_app.data.repository.RepositoryFactory

class KonmingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RepositoryFactory.initialize(this)
    }
}
