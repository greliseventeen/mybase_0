package com.example.konming_app.ui.mine

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.konming_app.R
import com.example.konming_app.data.repository.RepositoryFactory
import com.example.konming_app.ui.common.MainActivity
import com.example.konming_app.ui.login.LoginActivity

class AccountSwitchActivity : AppCompatActivity() {
    private lateinit var ivBack: ImageView
    private lateinit var rvAccounts: RecyclerView
    private lateinit var btnAddAccount: Button
    private lateinit var adapter: AccountSwitchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_switch)

        initViews()
        setupRecyclerView()
        loadAccounts()
    }

    private fun initViews() {
        ivBack = findViewById(R.id.iv_back)
        rvAccounts = findViewById(R.id.rv_accounts)
        btnAddAccount = findViewById(R.id.btn_add_account)

        ivBack.setOnClickListener {
            finish()
        }

        btnAddAccount.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        val currentUsername = RepositoryFactory.getPreferenceManager().username ?: ""
        adapter = AccountSwitchAdapter(
            currentUsername = currentUsername,
            onSwitchClick = { username, password ->
                showSwitchConfirmDialog(username, password)
            },
            onRemoveClick = { username ->
                showRemoveConfirmDialog(username)
            }
        )
        rvAccounts.layoutManager = LinearLayoutManager(this)
        rvAccounts.adapter = adapter
    }

    private fun loadAccounts() {
        val accounts = RepositoryFactory.getPreferenceManager().getSavedAccounts()
        adapter.submitList(accounts)
    }

    private fun showSwitchConfirmDialog(username: String, password: String) {
        AlertDialog.Builder(this)
            .setTitle("切换账号")
            .setMessage("确定切换到账号 $username 吗？")
            .setPositiveButton("确定") { _, _ ->
                // 登录
                val userRepository = RepositoryFactory.getUserRepository()
                val userId = userRepository.login(username, password)
                if (userId != null && userId > 0) {
                    val preferenceManager = RepositoryFactory.getPreferenceManager()
                    preferenceManager.saveLoginState(userId, username)
                    // 跳转到 MainActivity，清除栈
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showRemoveConfirmDialog(username: String) {
        AlertDialog.Builder(this)
            .setTitle("删除账号")
            .setMessage("确定删除账号 $username 吗？")
            .setPositiveButton("确定") { _, _ ->
                RepositoryFactory.getPreferenceManager().removeAccount(username)
                loadAccounts()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadAccounts()
    }
}
