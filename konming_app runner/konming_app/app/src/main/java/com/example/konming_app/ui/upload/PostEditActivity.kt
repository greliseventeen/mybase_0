package com.example.konming_app.ui.upload

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.konming_app.R
import com.example.konming_app.data.model.Post
import com.example.konming_app.data.repository.RepositoryFactory
import com.example.konming_app.ui.login.BaseAuthActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PostEditActivity : BaseAuthActivity() {

    private lateinit var ivBack: ImageView
    private lateinit var etContent: EditText
    private lateinit var btnAddImage: Button
    private lateinit var btnPublish: Button
    private lateinit var rvImages: RecyclerView

    private val imagePaths = mutableListOf<String>()
    private lateinit var imageThumbAdapter: ImageThumbAdapter
    private var tempImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_edit)

        initViews()
        setupRecyclerView()
        setupListeners()
    }

    private fun initViews() {
        ivBack = findViewById(R.id.ivBack)
        etContent = findViewById(R.id.etContent)
        btnAddImage = findViewById(R.id.btnAddImage)
        btnPublish = findViewById(R.id.btnPublish)
        rvImages = findViewById(R.id.rvImages)
    }

    private fun setupRecyclerView() {
        imageThumbAdapter = ImageThumbAdapter(imagePaths) { position ->
            imageThumbAdapter.removeAt(position)
        }
        rvImages.layoutManager = GridLayoutManager(this, 3)
        rvImages.adapter = imageThumbAdapter
    }

    private fun setupListeners() {
        ivBack.setOnClickListener {
            finish()
        }

        btnAddImage.setOnClickListener {
            showImageSelectionDialog()
        }

        btnPublish.setOnClickListener {
            handlePublish()
        }
    }

    private fun showImageSelectionDialog() {
        val options = arrayOf("拍照", "从相册选择")
        AlertDialog.Builder(this)
            .setTitle("选择图片")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndTakePhoto()
                    1 -> checkStoragePermissionAndSelectFromGallery()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            takePhoto()
        } else {
            showPermissionDialog("需要相机权限用于拍摄图片", Manifest.permission.CAMERA) {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkStoragePermissionAndSelectFromGallery() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isEmpty()) {
            selectFromGallery()
        } else {
            showPermissionDialog("需要存储权限用于选择图片", permissionsToRequest.toTypedArray()) {
                requestStoragePermission.launch(permissionsToRequest.toTypedArray())
            }
        }
    }

    private fun showPermissionDialog(message: String, permissions: Array<String>, onGrant: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("权限请求")
            .setMessage(message)
            .setPositiveButton("确定") { _, _ ->
                onGrant()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showPermissionDialog(message: String, permission: String, onGrant: () -> Unit) {
        showPermissionDialog(message, arrayOf(permission), onGrant)
    }

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            takePhoto()
        } else {
            Toast.makeText(this, "权限被拒绝，无法使用该功能", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestStoragePermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.all { it }) {
            selectFromGallery()
        } else {
            Toast.makeText(this, "权限被拒绝，无法使用该功能", Toast.LENGTH_SHORT).show()
        }
    }

    private fun takePhoto() {
        val imageFile = createTempFile()
        tempImageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            imageFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, tempImageUri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        takePhotoLauncher.launch(intent)
    }

    private fun selectFromGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        selectImageLauncher.launch(intent)
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            tempImageUri?.let { uri ->
                CoroutineScope(Dispatchers.IO).launch {
                    val savedPath = copyFileToAppStorage(uri)
                    withContext(Dispatchers.Main) {
                        if (savedPath != null) {
                            imageThumbAdapter.add(savedPath)
                        }
                    }
                }
            }
        }
    }

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.let { data ->
                val uris = mutableListOf<Uri>()
                
                if (data.clipData != null) {
                    val clipData = data.clipData!!
                    for (i in 0 until clipData.itemCount) {
                        uris.add(clipData.getItemAt(i).uri)
                    }
                } else if (data.data != null) {
                    uris.add(data.data!!)
                }

                if (uris.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val savedPaths = mutableListOf<String>()
                        for (uri in uris) {
                            val path = copyFileToAppStorage(uri)
                            path?.let { savedPaths.add(it) }
                        }
                        
                        withContext(Dispatchers.Main) {
                            if (savedPaths.isNotEmpty()) {
                                imageThumbAdapter.addAll(savedPaths)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createTempFile(): File {
        val dir = File(getExternalFilesDir(null), "images")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "${System.currentTimeMillis()}.jpg")
    }

    private suspend fun copyFileToAppStorage(sourceUri: Uri): String? {
        return try {
            val dir = File(getExternalFilesDir(null), "images")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val destFile = File(dir, "${System.currentTimeMillis()}.jpg")

            contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun handlePublish() {
        val content = etContent.text.toString().trim()

        if (TextUtils.isEmpty(content)) {
            showToast("请输入内容")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferenceManager = RepositoryFactory.getPreferenceManager()
                val userId = preferenceManager.getLoggedInUserId()

                val imagePathsStr = if (imagePaths.isNotEmpty()) {
                    imagePaths.joinToString(";")
                } else {
                    ""
                }

                val post = Post(
                    id = 0,
                    userId = userId,
                    content = content,
                    imagePaths = imagePathsStr,
                    timestamp = System.currentTimeMillis()
                )

                val postRepository = RepositoryFactory.getPostRepository()
                val postId = postRepository.insertPost(post)

                withContext(Dispatchers.Main) {
                    if (postId > 0) {
                        showToast("发布成功")
                        finish()
                    } else {                        showToast("发布失败，请重试")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("发布失败，请稍后重试")
                }
            }
        }
    }
}
