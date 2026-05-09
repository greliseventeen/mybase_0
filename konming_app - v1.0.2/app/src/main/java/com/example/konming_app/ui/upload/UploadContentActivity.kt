package com.example.konming_app.ui.upload

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.example.konming_app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class UploadContentActivity : AppCompatActivity() {
    private lateinit var viewModel: UploadContentViewModel

    // UI 组件
    private lateinit var ivBack: ImageView
    private lateinit var tilTitle: TextInputLayout
    private lateinit var etTitle: TextInputEditText
    private lateinit var tilCategory: TextInputLayout
    private lateinit var tvCategory: AutoCompleteTextView
    private lateinit var btnCaptureVideo: MaterialButton
    private lateinit var btnRecordAudio: MaterialButton
    private lateinit var btnSelectFromGallery: MaterialButton
    private lateinit var layoutFilePreview: LinearLayout
    private lateinit var videoView: VideoView
    private lateinit var layoutAudioPreview: LinearLayout
    private lateinit var tvAudioFilename: TextView
    private lateinit var layoutCoverSelection: LinearLayout
    private lateinit var ivCover: ImageView
    private lateinit var btnSelectCover: Button
    private lateinit var tilDescription: TextInputLayout
    private lateinit var etDescription: TextInputEditText
    private lateinit var btnPublish: Button

    // 数据成员
    private var selectedCategory = "经济"
    private var selectedFilePath: String = ""
    private var selectedFileType: String = ""
    private var coverImagePath: String = ""
    private var tempVideoUri: Uri? = null
    private var tempCoverUri: Uri? = null

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, UploadContentActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_content)

        viewModel = ViewModelProvider(this)[UploadContentViewModel::class.java]

        initViews()
        setupCategorySpinner()
        setupObservers()
        setupListeners()
    }

    private fun initViews() {
        ivBack = findViewById(R.id.ivBack)
        tilTitle = findViewById(R.id.tilTitle)
        etTitle = findViewById(R.id.etTitle)
        tilCategory = findViewById(R.id.tilCategory)
        tvCategory = findViewById(R.id.tvCategory)
        btnCaptureVideo = findViewById(R.id.btnCaptureVideo)
        btnRecordAudio = findViewById(R.id.btnRecordAudio)
        btnSelectFromGallery = findViewById(R.id.btnSelectFromGallery)
        layoutFilePreview = findViewById(R.id.layoutFilePreview)
        videoView = findViewById(R.id.videoView)
        layoutAudioPreview = findViewById(R.id.layoutAudioPreview)
        tvAudioFilename = findViewById(R.id.tvAudioFilename)
        layoutCoverSelection = findViewById(R.id.layoutCoverSelection)
        ivCover = findViewById(R.id.ivCover)
        btnSelectCover = findViewById(R.id.btnSelectCover)
        tilDescription = findViewById(R.id.tilDescription)
        etDescription = findViewById(R.id.etDescription)
        btnPublish = findViewById(R.id.btnPublish)
        
        // 设置默认封面
        ivCover.setImageResource(R.drawable.default_cover)
    }

    private fun setupCategorySpinner() {
        val categories = resources.getStringArray(R.array.content_categories)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, categories)
        tvCategory.setAdapter(adapter)
        tvCategory.setText(selectedCategory, false)
        
        tvCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = categories[position]
        }
    }

    private fun setupObservers() {
        viewModel.isUploading.observe(this) { isUploading ->
            btnPublish.isEnabled = !isUploading
            btnPublish.text = if (isUploading) "发布中..." else "发布"
        }

        viewModel.uploadResult.observe(this) { result ->
            result?.let {
                if (it) {
                    Toast.makeText(this, "上传成功", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    viewModel.errorMessage.value?.let { msg ->
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        ivBack.setOnClickListener {
            finish()
        }

        btnCaptureVideo.setOnClickListener {
            checkCameraPermissionAndCaptureVideo()
        }

        btnRecordAudio.setOnClickListener {
            checkRecordAudioPermission()
        }

        btnSelectFromGallery.setOnClickListener {
            checkStoragePermissionAndSelectFile()
        }

        btnSelectCover.setOnClickListener {
            showCoverSelectionDialog()
        }

        btnPublish.setOnClickListener {
            publishContent()
        }
    }

    // ==================== 权限检查和请求 ====================

    private fun checkCameraPermissionAndCaptureVideo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED) {
            captureVideo()
        } else {
            showPermissionDialog("需要相机权限用于拍摄视频", Manifest.permission.CAMERA) {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkRecordAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            == PackageManager.PERMISSION_GRANTED) {
            recordAudio()
        } else {
            showPermissionDialog("需要录音权限用于录制音频", Manifest.permission.RECORD_AUDIO) {
                requestRecordAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun checkStoragePermissionAndSelectFile() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isEmpty()) {
            selectFileFromGallery()
        } else {
            showPermissionDialog("需要存储权限用于选择文件", permissionsToRequest.toTypedArray()) {
                requestStoragePermission.launch(permissionsToRequest.toTypedArray())
            }
        }
    }

    private fun checkImagePermissionForCover() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isEmpty()) {
            selectCoverFromGallery()
        } else {
            showPermissionDialog("需要存储权限用于选择封面图片", permissionsToRequest.toTypedArray()) {
                requestImagePermission.launch(permissionsToRequest.toTypedArray())
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

    // 权限请求 Launchers
    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            captureVideo()
        } else {
            Toast.makeText(this, "权限被拒绝，无法使用该功能", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestRecordAudioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            recordAudio()
        } else {
            Toast.makeText(this, "权限被拒绝，无法使用该功能", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestStoragePermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.all { it }) {
            selectFileFromGallery()
        } else {
            Toast.makeText(this, "权限被拒绝，无法使用该功能", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestImagePermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.all { it }) {
            selectCoverFromGallery()
        } else {
            Toast.makeText(this, "权限被拒绝，无法使用该功能", Toast.LENGTH_SHORT).show()
        }
    }

    // ==================== 文件选择 ====================

    private fun captureVideo() {
        val videoFile = createTempFile("video", ".mp4")
        tempVideoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            videoFile
        )

        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, tempVideoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        captureVideoLauncher.launch(intent)
    }

    private fun recordAudio() {
        val intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
        recordAudioLauncher.launch(intent)
    }

    private fun selectFileFromGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*,audio/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("video/*", "audio/*"))
        }
        selectFileLauncher.launch(intent)
    }

    // 文件选择结果 Launchers
    private val captureVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            tempVideoUri?.let { uri ->
                CoroutineScope(Dispatchers.IO).launch {
                    val savedPath = copyFileToAppStorage(uri, "videos", ".mp4")
                    withContext(Dispatchers.Main) {
                        if (savedPath != null) {
                            selectedFilePath = savedPath
                            selectedFileType = "video"
                            showVideoPreview(savedPath)
                        }
                    }
                }
            }
        }
    }

    private val recordAudioLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                CoroutineScope(Dispatchers.IO).launch {
                    val savedPath = copyFileToAppStorage(uri, "audios", ".mp3")
                    withContext(Dispatchers.Main) {
                        if (savedPath != null) {
                            selectedFilePath = savedPath
                            selectedFileType = "audio"
                            showAudioPreview(savedPath)
                        }
                    }
                }
            }
        }
    }

    private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                CoroutineScope(Dispatchers.IO).launch {
                    val mimeType = contentResolver.getType(uri)
                    val isVideo = mimeType?.startsWith("video/") == true
                    val folder = if (isVideo) "videos" else "audios"
                    val ext = if (isVideo) ".mp4" else ".mp3"
                    
                    val savedPath = copyFileToAppStorage(uri, folder, ext)
                    withContext(Dispatchers.Main) {
                        if (savedPath != null) {
                            selectedFilePath = savedPath
                            selectedFileType = if (isVideo) "video" else "audio"
                            if (isVideo) {
                                showVideoPreview(savedPath)
                            } else {
                                showAudioPreview(savedPath)
                            }
                        }
                    }
                }
            }
        }
    }

    // ==================== 预览 ====================

    private fun showVideoPreview(path: String) {
        layoutFilePreview.visibility = LinearLayout.VISIBLE
        videoView.visibility = VideoView.VISIBLE
        layoutAudioPreview.visibility = LinearLayout.GONE
        layoutCoverSelection.visibility = LinearLayout.VISIBLE

        videoView.setVideoPath(path)
        videoView.start()
    }

    private fun showAudioPreview(path: String) {
        layoutFilePreview.visibility = LinearLayout.VISIBLE
        videoView.visibility = VideoView.GONE
        layoutAudioPreview.visibility = LinearLayout.VISIBLE
        layoutCoverSelection.visibility = LinearLayout.VISIBLE

        val file = File(path)
        tvAudioFilename.text = file.name
    }

    // ==================== 封面选择 ====================

    private fun showCoverSelectionDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(R.layout.dialog_cover_selection)
        
        val llGallery = bottomSheetDialog.findViewById<LinearLayout>(R.id.llGallery)
        val llCamera = bottomSheetDialog.findViewById<LinearLayout>(R.id.llCamera)
        val llDefault = bottomSheetDialog.findViewById<LinearLayout>(R.id.llDefault)
        
        llGallery?.setOnClickListener {
            bottomSheetDialog.dismiss()
            checkImagePermissionForCover()
        }
        
        llCamera?.setOnClickListener {
            bottomSheetDialog.dismiss()
            checkCameraPermissionAndTakeCover()
        }
        
        llDefault?.setOnClickListener {
            bottomSheetDialog.dismiss()
            useDefaultCover()
        }
        
        bottomSheetDialog.show()
    }

    private fun useDefaultCover() {
        coverImagePath = ""
        ivCover.setImageResource(R.drawable.default_cover)
    }

    private fun checkCameraPermissionAndTakeCover() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED) {
            takeCoverPhoto()
        } else {
            showPermissionDialog("需要相机权限用于拍照", Manifest.permission.CAMERA) {
                requestCoverCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private val requestCoverCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            takeCoverPhoto()
        } else {
            Toast.makeText(this, "权限被拒绝，无法使用该功能", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectCoverFromGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        selectCoverLauncher.launch(intent)
    }

    private fun takeCoverPhoto() {
        val coverFile = createTempFile("cover", ".jpg")
        tempCoverUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            coverFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, tempCoverUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        takeCoverLauncher.launch(intent)
    }

    // 封面选择结果 Launchers
    private val selectCoverLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                CoroutineScope(Dispatchers.IO).launch {
                    val savedPath = copyFileToAppStorage(uri, "covers", ".jpg")
                    withContext(Dispatchers.Main) {
                        if (savedPath != null) {
                            coverImagePath = savedPath
                            ivCover.setImageURI(Uri.fromFile(File(savedPath)))
                        }
                    }
                }
            }
        }
    }

    private val takeCoverLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            tempCoverUri?.let { uri ->
                CoroutineScope(Dispatchers.IO).launch {
                    val savedPath = copyFileToAppStorage(uri, "covers", ".jpg")
                    withContext(Dispatchers.Main) {
                        if (savedPath != null) {
                            coverImagePath = savedPath
                            ivCover.setImageURI(Uri.fromFile(File(savedPath)))
                        }
                    }
                }
            }
        }
    }

    // ==================== 文件存储 ====================

    private fun createTempFile(folder: String, extension: String): File {
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.parentFile
        val tempDir = File(dir, folder)
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        return File(tempDir, "${System.currentTimeMillis()}$extension")
    }

    private suspend fun copyFileToAppStorage(sourceUri: Uri, targetFolder: String, extension: String): String? {
        return try {
            val dir = File(getExternalFilesDir(null), targetFolder)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val destFile = File(dir, "${System.currentTimeMillis()}$extension")
            
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

    // ==================== 发布 ====================

    private fun publishContent() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "请输入标题", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedFilePath.isEmpty()) {
            Toast.makeText(this, "请选择文件", Toast.LENGTH_SHORT).show()
            return
        }

        if (title.length > 50) {
            tilTitle.error = "标题不能超过50字符"
            return
        }

        if (description.length > 500) {
            tilDescription.error = "简介不能超过500字符"
            return
        }

        tilTitle.error = null
        tilDescription.error = null

        viewModel.publishContent(
            title = title,
            category = selectedCategory,
            description = description,
            filePath = selectedFilePath,
            coverPath = coverImagePath,
            fileType = selectedFileType
        )
    }
}
