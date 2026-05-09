package com.example.konming_app.ui.home

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
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
        private const val HIDE_CONTROLS_DELAY = 3000L
    }

    private val fullscreenLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data!!
            val position = data.getLongExtra(FullscreenVideoActivity.EXTRA_PLAYBACK_POSITION, 0L)
            val playWhenReady = data.getBooleanExtra(FullscreenVideoActivity.EXTRA_PLAY_WHEN_READY, false)
            exoPlayer?.seekTo(position)
            if (playWhenReady) {
                exoPlayer?.play()
            }
        }
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
    private lateinit var frameVideo: android.widget.FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var layoutVideoControls: android.widget.LinearLayout
    private lateinit var ivVideoPlayPause: ImageView
    private lateinit var videoSeekBar: SeekBar
    private lateinit var tvVideoCurrentTime: TextView
    private lateinit var tvVideoTotalTime: TextView
    private lateinit var ivFullscreen: ImageView
    private lateinit var layoutAudio: android.widget.LinearLayout
    private lateinit var frameAudioCover: android.widget.FrameLayout
    private lateinit var ivCoverAudio: ImageView
    private lateinit var layoutAudioControls: android.widget.LinearLayout
    private lateinit var ivPlayPause: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private var videoUpdateRunnable: Runnable? = null
    private lateinit var layoutArticle: android.widget.LinearLayout
    private lateinit var ivCoverArticle: ImageView
    private lateinit var tvContentTitle: TextView
    private lateinit var tvContentCategory: TextView
    private lateinit var tvContentType: TextView
    private lateinit var tvContentDesc: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var updateSeekBarRunnable: Runnable? = null
    private var hideControlsRunnable: Runnable? = null

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
        frameVideo = findViewById(R.id.frame_video)
        playerView = findViewById(R.id.player_view)
        layoutVideoControls = findViewById(R.id.layout_video_controls)
        ivVideoPlayPause = findViewById(R.id.iv_video_play_pause)
        videoSeekBar = findViewById(R.id.video_seek_bar)
        tvVideoCurrentTime = findViewById(R.id.tv_video_current_time)
        tvVideoTotalTime = findViewById(R.id.tv_video_total_time)
        ivFullscreen = findViewById(R.id.iv_fullscreen)
        layoutAudio = findViewById(R.id.layout_audio)
        frameAudioCover = findViewById(R.id.frame_audio_cover)
        ivCoverAudio = findViewById(R.id.iv_cover_audio)
        layoutAudioControls = findViewById(R.id.layout_audio_controls)
        ivPlayPause = findViewById(R.id.iv_play_pause)
        seekBar = findViewById(R.id.seek_bar)
        tvCurrentTime = findViewById(R.id.tv_current_time)
        tvTotalTime = findViewById(R.id.tv_total_time)
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

        ivVideoPlayPause.setOnClickListener {
            toggleVideoPlayPause()
        }

        frameAudioCover.setOnClickListener {
            toggleAudioControlsVisibility()
        }

        frameVideo.setOnClickListener {
            toggleVideoControlsVisibility()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                cancelHideControls()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                scheduleHideControls()
            }
        })

        videoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    exoPlayer?.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                cancelHideControls()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                scheduleHideControls()
            }
        })
    }

    private fun toggleVideoControlsVisibility() {
        if (layoutVideoControls.visibility == android.view.View.VISIBLE) {
            layoutVideoControls.visibility = android.view.View.GONE
            cancelHideControls()
        } else {
            layoutVideoControls.visibility = android.view.View.VISIBLE
            scheduleHideControls()
        }
    }

    private fun toggleVideoPlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                cancelHideControls()
            } else {
                player.play()
                layoutVideoControls.visibility = android.view.View.VISIBLE
                scheduleHideControls()
            }
            updateVideoPlayPauseIcon()
        }
    }

    private fun updateVideoPlayPauseIcon() {
        exoPlayer?.let { player ->
            ivVideoPlayPause.setImageResource(
                if (player.isPlaying) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            )
        }
    }

    private fun startUpdateVideoSeekBar() {
        val runnable = object : Runnable {
            override fun run() {
                exoPlayer?.let { player ->
                    videoSeekBar.progress = player.currentPosition.toInt()
                    tvVideoCurrentTime.text = formatVideoTime(player.currentPosition)
                    if (player.isPlaying) {
                        handler.postDelayed(this, 1000)
                    }
                }
            }
        }
        videoUpdateRunnable = runnable
        handler.post(runnable)
    }

    private fun formatVideoTime(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    private fun toggleAudioControlsVisibility() {
        if (layoutAudioControls.visibility == android.view.View.VISIBLE) {
            layoutAudioControls.visibility = android.view.View.GONE
            cancelHideControls()
        } else {
            layoutAudioControls.visibility = android.view.View.VISIBLE
            scheduleHideControls()
        }
    }

    private fun scheduleHideControls() {
        cancelHideControls()
        hideControlsRunnable = Runnable {
            layoutAudioControls.visibility = android.view.View.GONE
        }
        handler.postDelayed(hideControlsRunnable!!, HIDE_CONTROLS_DELAY)
    }

    private fun cancelHideControls() {
        hideControlsRunnable?.let {
            handler.removeCallbacks(it)
        }
    }

    private fun openFullscreenVideo() {
        videoUri?.let { uri ->
            val position = exoPlayer?.currentPosition ?: 0L
            val playWhenReady = exoPlayer?.isPlaying ?: false
            val isLandscape = content?.isLandscape ?: true
            
            exoPlayer?.pause()
            
            val intent = Intent(this, FullscreenVideoActivity::class.java).apply {
                putExtra(FullscreenVideoActivity.EXTRA_VIDEO_URI, uri)
                putExtra(FullscreenVideoActivity.EXTRA_PLAYBACK_POSITION, position)
                putExtra(FullscreenVideoActivity.EXTRA_PLAY_WHEN_READY, playWhenReady)
                putExtra(FullscreenVideoActivity.EXTRA_IS_LANDSCAPE, isLandscape)
            }
            fullscreenLauncher.launch(intent)
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
                
                player.addListener(object : androidx.media3.common.Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            androidx.media3.common.Player.STATE_READY -> {
                                videoSeekBar.max = player.duration.toInt()
                                tvVideoTotalTime.text = formatVideoTime(player.duration)
                                layoutVideoControls.visibility = android.view.View.VISIBLE
                                scheduleHideControls()
                                startUpdateVideoSeekBar()
                                updateVideoPlayPauseIcon()
                            }
                            androidx.media3.common.Player.STATE_ENDED -> {
                                cancelHideControls()
                                updateVideoPlayPauseIcon()
                            }
                        }
                    }
                })
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
                    cancelHideControls()
                }
                seekBar.max = duration
                updateTimeDisplay()
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
            if (isPlaying) {
                layoutAudioControls.visibility = android.view.View.VISIBLE
                scheduleHideControls()
            } else {
                cancelHideControls()
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

    private fun updatePlayPauseButton() {
        if (isPlaying) {
            ivPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            ivPlayPause.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    private fun startUpdateSeekBar() {
        val runnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        seekBar.progress = it.currentPosition
                        updateTimeDisplay()
                        handler.postDelayed(this, 1000)
                    }
                }
            }
        }
        updateSeekBarRunnable = runnable
        handler.post(runnable)
    }

    private fun updateTimeDisplay() {
        val current = mediaPlayer?.currentPosition ?: 0
        val total = mediaPlayer?.duration ?: 0
        tvCurrentTime.text = formatDuration(current)
        tvTotalTime.text = formatDuration(total)
    }

    private fun formatDuration(ms: Int): String {
        val seconds = ms / 1000
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
                updatePlayPauseButton()
            }
        }
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
        cancelHideControls()
    }

    override fun onStop() {
        super.onStop()
        updateSeekBarRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
        }
        videoUpdateRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelHideControls()
        updateSeekBarRunnable?.let {
            handler.removeCallbacks(it)
        }
        videoUpdateRunnable?.let {
            handler.removeCallbacks(it)
        }
        mediaPlayer?.release()
        mediaPlayer = null
        exoPlayer?.release()
        exoPlayer = null
    }
}
