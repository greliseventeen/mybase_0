package com.example.konming_app.ui.home

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.example.konming_app.R
import com.example.konming_app.data.model.Content
import com.example.konming_app.data.repository.ContentRepository
import com.example.konming_app.data.repository.RepositoryFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ContentDetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_CONTENT_ID = "content_id"
        private const val REQUEST_FULLSCREEN = 1001
    }

    private lateinit var contentRepository: ContentRepository
    private var content: Content? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var exoPlayer: ExoPlayer? = null
    private var videoUri: Uri? = null

    private lateinit var ivBack: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var layoutVideo: android.widget.LinearLayout
    private lateinit var playerView: PlayerView
    private lateinit var ivFullscreen: ImageView
    private lateinit var layoutAudio: android.widget.LinearLayout
    private lateinit var ivCoverAudio: ImageView
    private lateinit var ivPlayPause: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var tvTime: TextView
    private lateinit var layoutArticle: android.widget.LinearLayout
    private lateinit var ivCoverArticle: ImageView
    private lateinit var tvContentTitle: TextView
    private lateinit var tvContentCategory: TextView
    private lateinit var tvContentType: TextView
    private lateinit var tvContentDesc: TextView

    private var updateSeekBarRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content_detail)

        contentRepository = RepositoryFactory.getContentRepository()
        val contentId = intent.getIntExtra(EXTRA_CONTENT_ID, -1)

        initViews()
        if (contentId != -1) {
            loadContent(contentId)
        }
    }

    private fun initViews() {
        ivBack = findViewById(R.id.iv_back)
        tvTitle = findViewById(R.id.tv_title)
        layoutVideo = findViewById(R.id.layout_video)
        playerView = findViewById(R.id.player_view)
        ivFullscreen = findViewById(R.id.iv_fullscreen)
        layoutAudio = findViewById(R.id.layout_audio)
        ivCoverAudio = findViewById(R.id.iv_cover_audio)
        ivPlayPause = findViewById(R.id.iv_play_pause)
        seekBar = findViewById(R.id.seek_bar)
        tvTime = findViewById(R.id.tv_time)
        layoutArticle = findViewById(R.id.layout_article)
        ivCoverArticle = findViewById(R.id.iv_cover_article)
        tvContentTitle = findViewById(R.id.tv_content_title)
        tvContentCategory = findViewById(R.id.tv_content_category)
        tvContentType = findViewById(R.id.tv_content_type)
        tvContentDesc = findViewById(R.id.tv_content_desc)

        ivBack.setOnClickListener {
            finish()
        }

        ivFullscreen.setOnClickListener {
            openFullscreenVideo()
        }

        ivPlayPause.setOnClickListener {
            togglePlayPause()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun openFullscreenVideo() {
        videoUri?.let { uri ->
            val position = exoPlayer?.currentPosition ?: 0
            val playWhenReady = exoPlayer?.isPlaying ?: false
            
            exoPlayer?.pause()
            
            val intent = Intent(this, FullscreenVideoActivity::class.java).apply {
                putExtra(FullscreenVideoActivity.EXTRA_VIDEO_URI, uri)
                putExtra(FullscreenVideoActivity.EXTRA_PLAYBACK_POSITION, position)
                putExtra(FullscreenVideoActivity.EXTRA_PLAY_WHEN_READY, playWhenReady)
            }
            startActivityForResult(intent, REQUEST_FULLSCREEN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FULLSCREEN && resultCode == Activity.RESULT_OK) {
            val position = data?.getLongExtra(FullscreenVideoActivity.EXTRA_PLAYBACK_POSITION, 0) ?: 0
            val playWhenReady = data?.getBooleanExtra(FullscreenVideoActivity.EXTRA_PLAY_WHEN_READY, false) ?: false
            
            exoPlayer?.seekTo(position)
            if (playWhenReady) {
                exoPlayer?.play()
            }
        }
    }

    private fun loadContent(contentId: Int) {
        lifecycleScope.launch {
            content = withContext(Dispatchers.IO) {
                contentRepository.getContentById(contentId)
            }
            
            content?.let {
                setupUI(it)
                val userId = RepositoryFactory.getPreferenceManager().getLoggedInUserId()
                if (userId != -1) {
                    withContext(Dispatchers.IO) {
                        RepositoryFactory.getBrowseHistoryRepository().addContentHistory(userId, it.id)
                    }
                }
            }
        }
    }

    private fun setupUI(content: Content) {
        tvTitle.text = content.title
        tvContentTitle.text = content.title
        tvContentCategory.text = content.category ?: "其他"
        tvContentType.text = when (content.type) {
            "video" -> "视频"
            "audio" -> "音频"
            else -> "文章"
        }
        tvContentDesc.text = content.desc ?: ""

        when (content.type) {
            "video" -> {
                layoutVideo.visibility = android.view.View.VISIBLE
                layoutAudio.visibility = android.view.View.GONE
                layoutArticle.visibility = android.view.View.GONE
                setupVideo(content)
            }
            "audio" -> {
                layoutVideo.visibility = android.view.View.GONE
                layoutAudio.visibility = android.view.View.VISIBLE
                layoutArticle.visibility = android.view.View.GONE
                setupAudio(content)
            }
            else -> {
                layoutVideo.visibility = android.view.View.GONE
                layoutAudio.visibility = android.view.View.GONE
                layoutArticle.visibility = android.view.View.VISIBLE
                setupArticle(content)
            }
        }
    }

    private fun setupVideo(content: Content) {
        val filePath = content.filePath ?: return
        val file = File(filePath)
        if (file.exists()) {
            videoUri = Uri.fromFile(file)
            exoPlayer = ExoPlayer.Builder(this).build().also { player ->
                playerView.player = player
                val mediaItem = MediaItem.fromUri(videoUri!!)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
            }
        }
    }

    private fun setupAudio(content: Content) {
        val coverPath = content.coverPath
        if (!coverPath.isNullOrEmpty()) {
            val coverFile = File(coverPath)
            if (coverFile.exists()) {
                Glide.with(this)
                    .load(coverFile)
                    .placeholder(R.drawable.default_cover)
                    .centerCrop()
                    .into(ivCoverAudio)
            }
        }
        
        val filePath = content.filePath ?: return
        val file = File(filePath)
        if (file.exists()) {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    this@ContentDetailActivity.isPlaying = false
                    updatePlayPauseButton()
                }
                seekBar.max = duration
                updateTimeDisplay()
            }
        }
    }

    private fun setupArticle(content: Content) {
        val coverPath = content.coverPath
        if (!coverPath.isNullOrEmpty()) {
            val coverFile = File(coverPath)
            if (coverFile.exists()) {
                Glide.with(this)
                    .load(coverFile)
                    .placeholder(R.drawable.default_cover)
                    .centerCrop()
                    .into(ivCoverArticle)
            }
        }
    }

    private fun togglePlayPause() {
        mediaPlayer?.let {
            if (isPlaying) {
                it.pause()
            } else {
                it.start()
                startUpdateSeekBar()
            }
            isPlaying = !isPlaying
            updatePlayPauseButton()
        }
    }

    private fun updatePlayPauseButton() {
        if (isPlaying) {
            ivPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            ivPlayPause.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    private fun startUpdateSeekBar() {
        updateSeekBarRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        seekBar.progress = it.currentPosition
                        updateTimeDisplay()
                        ivPlayPause.postDelayed(this, 1000)
                    }
                }
            }
        }
        ivPlayPause.post(updateSeekBarRunnable)
    }

    private fun updateTimeDisplay() {
        val current = mediaPlayer?.currentPosition ?: 0
        val total = mediaPlayer?.duration ?: 0
        tvTime.text = "${formatDuration(current)}/${formatDuration(total)}"
    }

    private fun formatDuration(ms: Int): String {
        val seconds = ms / 1000
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
            updateSeekBarRunnable?.let { runnable ->
                ivPlayPause.removeCallbacks(runnable)
            }
        }
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        exoPlayer?.release()
        exoPlayer = null
    }
}
