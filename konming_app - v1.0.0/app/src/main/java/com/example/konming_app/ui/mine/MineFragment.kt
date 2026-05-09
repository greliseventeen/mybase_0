package com.example.konming_app.ui.mine

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.konming_app.R
import com.example.konming_app.ui.browse.BrowseHistoryActivity
import com.example.konming_app.ui.login.LoginActivity
import com.example.konming_app.ui.mine.favorite.FavoriteListActivity
import com.example.konming_app.ui.mine.publish.MyPublishActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MineFragment : Fragment() {
    private lateinit var viewModel: MineViewModel

    private lateinit var ivAvatar: ImageView
    private lateinit var tvNickname: TextView
    private lateinit var ivEditNickname: ImageView
    private lateinit var tvBio: TextView
    private lateinit var ivEditBio: ImageView
    private lateinit var tvUserId: TextView
    private lateinit var tvIp: TextView
    private lateinit var btnChangePassword: Button

    private var tempCameraUri: Uri? = null

    companion object {
        private const val REQUEST_CODE_CAMERA_PERMISSION = 1001
        private const val REQUEST_CODE_STORAGE_PERMISSION = 1002
    }

    // 相机和相册的 ActivityResultLauncher
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            tempCameraUri?.let { uri ->
                saveAndUpdateAvatar(uri)
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                saveAndUpdateAvatar(uri)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // 权限已授予，可以执行操作
        } else {
            Toast.makeText(requireContext(), "权限被拒绝", Toast.LENGTH_SHORT).show()
        }
    }

    private lateinit var menuFavorites: LinearLayout
    private lateinit var menuMyPosts: LinearLayout
    private lateinit var menuHistory: LinearLayout
    private lateinit var menuSwitchAccount: LinearLayout
    private lateinit var menuLogout: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mine, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[MineViewModel::class.java]
        
        initViews(view)
        setupObservers()
        setupClickListeners()
    }

    private fun initViews(view: View) {
        ivAvatar = view.findViewById(R.id.iv_avatar)
        tvNickname = view.findViewById(R.id.tv_nickname)
        ivEditNickname = view.findViewById(R.id.iv_edit_nickname)
        tvBio = view.findViewById(R.id.tv_bio)
        ivEditBio = view.findViewById(R.id.iv_edit_bio)
        tvUserId = view.findViewById(R.id.tv_user_id)
        tvIp = view.findViewById(R.id.tv_ip)
        btnChangePassword = view.findViewById(R.id.btn_change_password)

        menuFavorites = view.findViewById(R.id.menu_favorites)
        menuMyPosts = view.findViewById(R.id.menu_my_posts)
        menuHistory = view.findViewById(R.id.menu_history)
        menuSwitchAccount = view.findViewById(R.id.menu_switch_account)
        menuLogout = view.findViewById(R.id.menu_logout)
    }

    private fun setupObservers() {
        viewModel.userInfo.observe(viewLifecycleOwner) { user ->
            user?.let {
                tvNickname.text = it.nickname ?: it.username
                tvBio.text = if (it.bio.isNullOrEmpty()) "这个人很懒，什么都没写" else it.bio
                tvUserId.text = "用户ID: ${it.id}"
                
                if (!it.avatarPath.isNullOrEmpty()) {
                    val avatarFile = File(it.avatarPath)
                    if (avatarFile.exists()) {
                        Glide.with(this)
                            .load(avatarFile)
                            .placeholder(R.drawable.default_avatar)
                            .circleCrop()
                            .into(ivAvatar)
                    } else {
                        ivAvatar.setImageResource(R.drawable.default_avatar)
                    }
                } else {
                    ivAvatar.setImageResource(R.drawable.default_avatar)
                }
            }
        }

        viewModel.ipLocation.observe(viewLifecycleOwner) { location ->
            tvIp.text = "IP属地: $location"
        }

        viewModel.logoutEvent.observe(viewLifecycleOwner) { shouldLogout ->
            if (shouldLogout) {
                viewModel.onLogoutEventConsumed()
                val intent = Intent(requireActivity(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }

    private fun setupClickListeners() {
        ivAvatar.setOnClickListener {
            showAvatarOptionsDialog()
        }

        ivEditNickname.setOnClickListener {
            showEditNicknameDialog()
        }

        ivEditBio.setOnClickListener {
            showEditBioDialog()
        }

        btnChangePassword.setOnClickListener {
            startActivity(Intent(requireActivity(), ChangePasswordActivity::class.java))
        }

        menuFavorites.setOnClickListener {
            startActivity(Intent(requireActivity(), FavoriteListActivity::class.java))
        }

        menuMyPosts.setOnClickListener {
            startActivity(Intent(requireActivity(), MyPublishActivity::class.java))
        }

        menuHistory.setOnClickListener {
            startActivity(Intent(requireActivity(), BrowseHistoryActivity::class.java))
        }

        menuSwitchAccount.setOnClickListener {
            startActivity(Intent(requireActivity(), AccountSwitchActivity::class.java))
        }

        menuLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    private fun showEditNicknameDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_text, null)
        val etInput = dialogView.findViewById<EditText>(R.id.et_input)
        etInput.setText(tvNickname.text)
        etInput.hint = "请输入昵称"

        AlertDialog.Builder(requireContext())
            .setTitle("修改昵称")
            .setView(dialogView)
            .setPositiveButton("确定") { _, _ ->
                val newNickname = etInput.text.toString().trim()
                if (newNickname.isNotEmpty()) {
                    viewModel.updateNickname(newNickname)
                    Toast.makeText(requireContext(), "修改成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "昵称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditBioDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_text, null)
        val etInput = dialogView.findViewById<EditText>(R.id.et_input)
        val currentBio = tvBio.text.toString()
        if (currentBio != "这个人很懒，什么都没写") {
            etInput.setText(currentBio)
        }
        etInput.hint = "请输入个性签名"

        AlertDialog.Builder(requireContext())
            .setTitle("修改个性签名")
            .setView(dialogView)
            .setPositiveButton("确定") { _, _ ->
                val newBio = etInput.text.toString().trim()
                viewModel.updateBio(newBio)
                Toast.makeText(requireContext(), "修改成功", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showLogoutConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("退出登录")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("确定") { _, _ ->
                viewModel.logout()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshUserInfo()
    }

    private fun showAvatarOptionsDialog() {
        val options = arrayOf("拍照", "从相册选择", "取消")
        AlertDialog.Builder(requireContext())
            .setTitle("选择头像")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpenCamera()
                    1 -> checkStoragePermissionAndOpenGallery()
                    2 -> {}
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndOpenCamera() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("需要相机权限")
                    .setMessage("需要相机权限拍摄头像，请授予权限")
                    .setPositiveButton("确定") { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkStoragePermissionAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("需要存储权限")
                    .setMessage("需要存储权限选择头像，请授予权限")
                    .setPositiveButton("确定") { _, _ ->
                        requestPermissionLauncher.launch(permission)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = createTempImageFile()
        tempCameraUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        intent.putExtra(MediaStore.EXTRA_OUTPUT, tempCameraUri)
        cameraLauncher.launch(intent)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        galleryLauncher.launch(intent)
    }

    private fun createTempImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "IMG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun saveAndUpdateAvatar(sourceUri: Uri) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val userId = viewModel.userId
                val avatarsDir = File(requireContext().getExternalFilesDir(null), "avatars")
                if (!avatarsDir.exists()) {
                    avatarsDir.mkdirs()
                }
                val avatarFile = File(avatarsDir, "avatar_$userId.jpg")

                // 从 sourceUri 复制到 avatarFile
                val inputStream = requireContext().contentResolver.openInputStream(sourceUri)
                val outputStream = FileOutputStream(avatarFile)
                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                // 更新 ViewModel
                withContext(Dispatchers.Main) {
                    viewModel.updateAvatarPath(avatarFile.absolutePath)
                    Toast.makeText(requireContext(), "头像更新成功", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "头像更新失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
