package com.example.konming_app.ui.login

import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import com.example.konming_app.R
import com.example.konming_app.data.repository.RepositoryFactory
import com.example.konming_app.ui.common.MainActivity
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : BaseAuthActivity() {

    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            handleLogin()
        }

        tvRegister.setOnClickListener {
            navigateToActivity(RegisterActivity::class.java, finishCurrent = false)
        }
    }

    override fun onBackPressedAction() {
        moveTaskToBack(true)
    }

    private fun handleLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (TextUtils.isEmpty(username)) {
            showToast("请输入用户名")
            return
        }

        if (TextUtils.isEmpty(password)) {
            showToast("请输入密码")
            return
        }

        val userRepository = RepositoryFactory.getUserRepository()
        val userId = userRepository.login(username, password)

        if (userId != null && userId > 0) {
            val preferenceManager = RepositoryFactory.getPreferenceManager()
            preferenceManager.saveLoginState(userId, username)
            preferenceManager.saveAccount(username, password)

            navigateToActivity(MainActivity::class.java)
        } else {
            showAlertDialog("提示", "账号或密码错误")
        }
    }
}
