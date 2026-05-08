package com.example.konming_app.ui.mine

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.konming_app.R
import com.example.konming_app.data.repository.RepositoryFactory
import com.example.konming_app.data.repository.UserRepository
import com.example.konming_app.ui.login.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangePasswordActivity : AppCompatActivity() {
    private lateinit var userRepository: UserRepository
    private lateinit var preferenceManager: com.example.konming_app.data.local.PreferenceManager

    private lateinit var ivBack: ImageView
    private lateinit var etOldPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSave: Button

    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        userRepository = RepositoryFactory.getUserRepository()
        preferenceManager = RepositoryFactory.getPreferenceManager()
        userId = preferenceManager.getLoggedInUserId()

        initViews()
    }

    private fun initViews() {
        ivBack = findViewById(R.id.iv_back)
        etOldPassword = findViewById(R.id.et_old_password)
        etNewPassword = findViewById(R.id.et_new_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnSave = findViewById(R.id.btn_save)

        ivBack.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            changePassword()
        }
    }

    private fun changePassword() {
        val oldPassword = etOldPassword.text.toString().trim()
        val newPassword = etNewPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (oldPassword.isEmpty()) {
            Toast.makeText(this, "请输入原密码", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.isEmpty()) {
            Toast.makeText(this, "请输入新密码", Toast.LENGTH_SHORT).show()
            return
        }

        if (confirmPassword.isEmpty()) {
            Toast.makeText(this, "请确认新密码", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "两次输入的新密码不一致", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.length < 6) {
            Toast.makeText(this, "密码长度不能少于6位", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                val currentUser = userRepository.getUserInfo(userId)
                if (currentUser != null && currentUser.password == oldPassword) {
                    userRepository.updatePassword(userId, newPassword)
                    true
                } else {
                    false
                }
            }

            if (success) {
                Toast.makeText(this@ChangePasswordActivity, "修改成功，请重新登录", Toast.LENGTH_SHORT).show()
                preferenceManager.clearLoginState()
                val intent = Intent(this@ChangePasswordActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this@ChangePasswordActivity, "原密码错误", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
