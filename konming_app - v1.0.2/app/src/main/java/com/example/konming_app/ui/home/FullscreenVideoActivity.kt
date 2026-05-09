package com.example.konming_app.ui.home

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.konming_app.R

class FullscreenVideoActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_VIDEO_URI = "video_uri"
        const val EXTRA_PLAYBACK_POSITION = "playback_position"
        const val EXTRA_PLAY_WHEN_READY = "play_when_ready"
        const val EXTRA_IS_LANDSCAPE = "is_landscape"
        private const val HIDE_CONTROLS_DELAY = 3000L
        
        private val PLAYBACK_SPEEDS = arrayOf(0.5f, 1.0f, 1.5f, 2.0f)
        private val SPEED_LABELS = arrayOf("0.5x", "1.0x", "1.5x", "2.0x")
    }

    private var exoPlayer: ExoPlayer? = null
    private lateinit var fullscreenPlayerView: PlayerView
    private lateinit var frameFullscreenContainer: View
    private lateinit var layoutFullscreenControls: LinearLayout
    private lateinit var ivFullscreenPlayPause: ImageView
    private lateinit var fullscreenSeekBar: SeekBar
    private lateinit var tvFullscreenCurrentTime: TextView
    private lateinit var tvFullscreenTotalTime: TextView
    private lateinit var ivFullscreenSettings: ImageView
    private lateinit var layoutSpeedPopup: LinearLayout
    private lateinit var ivBackFullscreen: ImageView
    private lateinit var ivPortrait: ImageView
    private lateinit var ivLandscape: ImageView

    private var playbackPosition: Long = 0
    private var playWhenReady: Boolean = true
    private var videoUri: Uri? = null
    private var isLandscape: Boolean = false
    private var isControlsVisible: Boolean = true
    private var isSpeedPopupVisible: Boolean = false
    private var currentSpeedIndex = 1

    private val handler = Handler(Looper.getMainLooper())
    private var hideControlsRunnable: Runnable? = null
    private var updateSeekBarRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_video)

        videoUri = intent.getParcelableExtra(EXTRA_VIDEO_URI)
        playbackPosition = intent.getLongExtra(EXTRA_PLAYBACK_POSITION, 0)
        playWhenReady = intent.getBooleanExtra(EXTRA_PLAY_WHEN_READY, true)
        isLandscape = intent.getBooleanExtra(EXTRA_IS_LANDSCAPE, true)

        initViews()
        setFullscreen()
        updateOrientation()
        setupPlayer()
    }

    private fun initViews() {
        frameFullscreenContainer = findViewById(R.id.frame_fullscreen_container)
        fullscreenPlayerView = findViewById(R.id.fullscreen_player_view)
        layoutFullscreenControls = findViewById(R.id.layout_fullscreen_controls)
        ivFullscreenPlayPause = findViewById(R.id.iv_fullscreen_play_pause)
        fullscreenSeekBar = findViewById(R.id.fullscreen_seek_bar)
        tvFullscreenCurrentTime = findViewById(R.id.tv_fullscreen_current_time)
        tvFullscreenTotalTime = findViewById(R.id.tv_fullscreen_total_time)
        ivFullscreenSettings = findViewById(R.id.iv_fullscreen_settings)
        layoutSpeedPopup = findViewById(R.id.layout_speed_popup)
        ivBackFullscreen = findViewById(R.id.iv_back_fullscreen)
        ivPortrait = findViewById(R.id.iv_portrait)
        ivLandscape = findViewById(R.id.iv_landscape)

        frameFullscreenContainer.setOnClickListener {
            if (!isSpeedPopupVisible) {
                toggleControlsVisibility()
            }
        }

        ivBackFullscreen.setOnClickListener {
            finish()
        }

        ivFullscreenPlayPause.setOnClickListener {
            togglePlayPause()
        }

        ivFullscreenSettings.setOnClickListener {
            toggleSpeedPopup()
        }

        ivPortrait.setOnClickListener {
            isLandscape = false
            updateOrientation()
        }

        ivLandscape.setOnClickListener {
            isLandscape = true
            updateOrientation()
        }

        fullscreenSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    tvFullscreenCurrentTime.text = formatTime(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                cancelHideControls()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                exoPlayer?.seekTo(fullscreenSeekBar.progress.toLong())
                scheduleHideControls()
            }
        })

        setupSpeedButtons()
    }

    private fun setupSpeedButtons() {
        val speedButtons = listOf(
            findViewById<TextView>(R.id.tv_speed_0_5x),
            findViewById<TextView>(R.id.tv_speed_1_0x),
            findViewById<TextView>(R.id.tv_speed_1_5x),
            findViewById<TextView>(R.id.tv_speed_2_0x)
        )

        speedButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                setPlaybackSpeed(index)
            }
        }
    }

    private fun toggleSpeedPopup() {
        isSpeedPopupVisible = !isSpeedPopupVisible
        layoutSpeedPopup.visibility = if (isSpeedPopupVisible) View.VISIBLE else View.GONE
        
        if (isSpeedPopupVisible) {
            cancelHideControls()
        } else {
            scheduleHideControls()
        }
    }

    private fun setPlaybackSpeed(index: Int) {
        currentSpeedIndex = index
        exoPlayer?.setPlaybackSpeed(PLAYBACK_SPEEDS[index])
        
        val speedButtons = listOf(
            findViewById<TextView>(R.id.tv_speed_0_5x),
            findViewById<TextView>(R.id.tv_speed_1_0x),
            findViewById<TextView>(R.id.tv_speed_1_5x),
            findViewById<TextView>(R.id.tv_speed_2_0x)
        )

        speedButtons.forEachIndexed { i, button ->
            if (i == index) {
                button.setTextColor(resources.getColor(R.color.colorPrimary))
                button.setTypeface(button.typeface, android.graphics.Typeface.BOLD)
            } else {
                button.setTextColor(resources.getColor(android.R.color.black))
                button.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }

        toggleSpeedPopup()
    }

    private fun toggleControlsVisibility() {
        isControlsVisible = !isControlsVisible
        layoutFullscreenControls.visibility = if (isControlsVisible) View.VISIBLE else View.GONE
        
        if (isControlsVisible) {
            scheduleHideControls()
        } else {
            cancelHideControls()
        }
    }

    private fun scheduleHideControls() {
        cancelHideControls()
        hideControlsRunnable = Runnable {
            if (exoPlayer?.isPlaying == true && !isSpeedPopupVisible) {
                layoutFullscreenControls.visibility = View.GONE
                isControlsVisible = false
            }
        }
        handler.postDelayed(hideControlsRunnable!!, HIDE_CONTROLS_DELAY)
    }

    private fun cancelHideControls() {
        hideControlsRunnable?.let {
            handler.removeCallbacks(it)
            hideControlsRunnable = null
        }
    }

    private fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                cancelHideControls()
            } else {
                player.play()
                layoutFullscreenControls.visibility = View.VISIBLE
                isControlsVisible = true
                scheduleHideControls()
            }
            updatePlayPauseIcon()
        }
    }

    private fun updatePlayPauseIcon() {
        exoPlayer?.let { player ->
            ivFullscreenPlayPause.setImageResource(
                if (player.isPlaying) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            )
        }
    }

    private fun startUpdateSeekBar() {
        val runnable = object : Runnable {
            override fun run() {
                exoPlayer?.let { player ->
                    fullscreenSeekBar.progress = player.currentPosition.toInt()
                    tvFullscreenCurrentTime.text = formatTime(player.currentPosition)
                    if (player.isPlaying) {
                        handler.postDelayed(this, 1000)
                    }
                }
            }
        }
        updateSeekBarRunnable = runnable
        handler.post(runnable)
    }

    private fun formatTime(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    private fun updateOrientation() {
        requestedOrientation = if (isLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        
        ivPortrait.setColorFilter(
            if (!isLandscape) resources.getColor(R.color.colorPrimary) 
            else resources.getColor(android.R.color.white)
        )
        
        ivLandscape.setColorFilter(
            if (isLandscape) resources.getColor(R.color.colorPrimary) 
            else resources.getColor(android.R.color.white)
        )
    }

    private fun setFullscreen() {
        window.decorView.windowInsetsController?.let { controller ->
            controller.hide(WindowInsets.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        actionBar?.hide()
    }

    private fun setupPlayer() {
        videoUri?.let { uri ->
            exoPlayer = ExoPlayer.Builder(this).build().also { player ->
                fullscreenPlayerView.player = player
                val mediaItem = MediaItem.fromUri(uri)
                player.setMediaItem(mediaItem)
                player.seekTo(playbackPosition)
                player.playWhenReady = playWhenReady
                player.setPlaybackSpeed(PLAYBACK_SPEEDS[currentSpeedIndex])
                player.prepare()

                player.addListener(object : androidx.media3.common.Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            androidx.media3.common.Player.STATE_READY -> {
                                fullscreenSeekBar.max = player.duration.toInt()
                                tvFullscreenTotalTime.text = formatTime(player.duration)
                                startUpdateSeekBar()
                                updatePlayPauseIcon()
                                if (player.isPlaying) {
                                    scheduleHideControls()
                                }
                            }
                            androidx.media3.common.Player.STATE_ENDED -> {
                                cancelHideControls()
                                layoutFullscreenControls.visibility = View.VISIBLE
                                isControlsVisible = true
                                updatePlayPauseIcon()
                            }
                        }
                    }
                })
            }
        }
    }

    override fun finish() {
        exoPlayer?.let {
            val resultIntent = android.content.Intent().apply {
                putExtra(EXTRA_PLAYBACK_POSITION, it.currentPosition)
                putExtra(EXTRA_PLAY_WHEN_READY, it.isPlaying)
            }
            setResult(android.app.Activity.RESULT_OK, resultIntent)
        }
        super.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelHideControls()
        updateSeekBarRunnable?.let {
            handler.removeCallbacks(it)
        }
        exoPlayer?.release()
        exoPlayer = null
    }
}