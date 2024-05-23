package com.ismartcoding.plain.ui.components.mediaviewer.video

import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.util.Rational
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.exoplayer.ExoPlayer
import com.ismartcoding.lib.isTPlus
import com.ismartcoding.plain.audioManager

class VideoState {
    var isPlaying by mutableStateOf(false)
    var isMuted by mutableStateOf(false)
    var currentTime by mutableLongStateOf(0L)
    var totalTime by mutableLongStateOf(0L)
    var bufferedPercentage by mutableIntStateOf(0)
    var enablePip by mutableStateOf(false)
    var isFullscreenMode by mutableStateOf(false)
    var speed by mutableFloatStateOf(1f)
    var player: ExoPlayer? = null
    var isSeeking = false

    fun initData(player: ExoPlayer) {
        this.player = player
        if (isMuted) {
            player.volume = 0f
        }
        player.setPlaybackSpeed(speed)
    }

    fun changeSpeed(speed: Float) {
        this.speed = speed
        player?.setPlaybackSpeed(speed)
    }

    fun seekTo(position: Long) {
        isSeeking = true
        currentTime = position
        player?.seekTo(position)
        isSeeking = false
        if (!isPlaying) {
            player?.play()
        }
    }

    fun togglePlay() {
        if (isPlaying) {
            player?.pause()
        } else {
            if (currentTime >= totalTime) {
                currentTime = 0
                player?.seekTo(0)
            }
            player?.play()
        }
    }

    fun toggleMute() {
        if (isMuted) {
            player?.volume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM).toFloat()
            isMuted = false
        } else {
            player?.volume = 0f
            isMuted = true
        }
    }

    fun hasPipMode(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    fun enterPipMode(context: Context) {
        if (hasPipMode(context)) {
            enablePip = true
            val params = PictureInPictureParams.Builder()
            if (isTPlus()) {
                params
                    .setTitle("Video Player")
                    .setAspectRatio(Rational(16, 9))
                    .setSeamlessResizeEnabled(true)
            }

            context.findActivity().enterPictureInPictureMode(params.build())
        }
    }

    fun updateTime() {
        isPlaying = player?.isPlaying ?: false // Hacky way to update isPlaying
        currentTime = player?.currentPosition?.coerceAtLeast(0L) ?: 0L
        totalTime = player?.duration?.coerceAtLeast(0L) ?: 0L
        bufferedPercentage = player?.bufferedPercentage ?: 0
    }
}