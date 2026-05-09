package com.example.konming_app.ui.common

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.konming_app.R
import com.example.konming_app.data.repository.RepositoryFactory
import com.example.konming_app.ui.login.LoginActivity
import com.example.konming_app.ui.upload.PostEditActivity
import com.example.konming_app.ui.upload.UploadContentActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RepositoryFactory.initialize(this)

        val preferenceManager = RepositoryFactory.getPreferenceManager()
        val userId = preferenceManager.getLoggedInUserId()

        if (userId == -1) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        bottomNavView.setupWithNavController(navController)

        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.publishFragment -> {
                    showPublishOptionsDialog()
                    false
                }
                else -> {
                    val currentDestination = navController.currentDestination?.id
                    if (currentDestination != item.itemId) {
                        navController.navigate(item.itemId)
                    }
                    true
                }
            }
        }
    }

    private fun showPublishOptionsDialog() {
        val options = arrayOf("发帖", "上传音视频/文章")

        AlertDialog.Builder(this)
            .setTitle("选择发布内容")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        startActivity(Intent(this, PostEditActivity::class.java))
                    }
                    1 -> {
                        UploadContentActivity.start(this)
                    }
                }
            }
            .show()
    }
}
