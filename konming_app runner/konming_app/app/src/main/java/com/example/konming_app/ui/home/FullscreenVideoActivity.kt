package com.example.konming_app.ui.home

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageView
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
    }

    private var exoPlayer: ExoPlayer? = null
    private lateinit var fullscreenPlayerView: PlayerView
    private lateinit var ivBackFullscreen: ImageView
    private lateinit var ivPortrait: ImageView
    private lateinit var ivLandscape: ImageView

    private var playbackPosition: Long = 0
    private var playWhenReady: Boolean = true
    private var videoUri: Uri? = null
    private var isLandscape: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_video)

        videoUri = intent.getParcelableExtra(EXTRA_VIDEO_URI)
        playbackPosition = intent.getLongExtra(EXTRA_PLAYBACK_POSITION, 0)
        playWhenReady = intent.getBooleanExtra(EXTRA_PLAY_WHEN_READY, true)
        isLandscape = intent.getBooleanExtra("is_landscape", false)

        initViews()
        setFullscreen()
        updateOrientation()
        setupPlayer()
    }

    private fun initViews() {
        fullscreenPlayerView = findViewById(R.id.fullscreen_player_view)
        ivBackFullscreen = findViewById(R.id.iv_back_fullscreen)
        ivPortrait = findViewById(R.id.iv_portrait)
        ivLandscape = findViewById(R.id.iv_landscape)

        ivBackFullscreen.setOnClickListener {
            finish()
        }

        ivPortrait.setOnClickListener {
            isLandscape = false
            updateOrientation()
        }

        ivLandscape.setOnClickListener {
            isLandscape = true
            updateOrientation()
        }
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
                player.prepare()
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
        exoPlayer?.release()
        exoPlayer = null
    }
}
