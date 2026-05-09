package com.example.konming_app.ui.login

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.widget.Button
import android.widget.TextView
import com.example.konming_app.R
import com.example.konming_app.data.repository.RepositoryFactory
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class RegisterActivity : BaseAuthActivity() {

    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView

    private val usernamePattern = Pattern.compile("^[a-zA-Z0-9_]{4,16}$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)
    }

    private fun setupListeners() {
        btnRegister.setOnClickListener {
            handleRegister()
        }

        tvLogin.setOnClickListener {
            navigateToActivity(LoginActivity::class.java)
        }

        etUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateUsername(s.toString())
            }
        })

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePassword(s.toString())
            }
        })

        etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateConfirmPassword(s.toString())
            }
        })
    }

    override fun onBackPressedAction() {
        navigateToActivity(LoginActivity::class.java)
    }

    private fun validateUsername(username: String): Boolean {
        if (TextUtils.isEmpty(username)) {
            etUsername.error = "用户名不能为空"
            return false
        }

        if (!usernamePattern.matcher(username).matches()) {
            etUsername.error = "用户名必须为4-16位字母、数字或下划线"
            return false
        }

        etUsername.error = null
        return true
    }

    private fun validatePassword(password: String): Boolean {
        if (TextUtils.isEmpty(password)) {
            etPassword.error = "密码不能为空"
            return false
        }

        if (password.length < 6 || password.length > 20) {
            etPassword.error = "密码长度必须为6-20位"
            return false
        }

        etPassword.error = null
        return true
    }

    private fun validateConfirmPassword(confirmPassword: String): Boolean {
        val password = etPassword.text.toString()
        if (confirmPassword != password) {
            etConfirmPassword.error = "两次输入的密码不一致"
            return false
        }

        etConfirmPassword.error = null
        return true
    }

    private fun handleRegister() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (!validateUsername(username) || !validatePassword(password) || !validateConfirmPassword(confirmPassword)) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userRepository = RepositoryFactory.getUserRepository()
                val userId = userRepository.register(username, password)

                withContext(Dispatchers.Main) {
                    if (userId != null) {
                        showToast("注册成功，你的用户ID: $userId")

                        Handler(Looper.getMainLooper()).postDelayed({
                            navigateToActivity(LoginActivity::class.java)
                        }, 1500)
                    } else {
                        showAlertDialog("提示", "用户名已存在，请更换其他用户名")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("注册失败，请稍后重试")
                }
            }
        }
    }
}
