package com.ismartcoding.plain.ui.components.mediaviewer.video

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Pure commonMain state holder for video playback.
 * Platform-specific player interactions are delegated to [VideoPlayerController].
 */
class VideoState {
    var isPlaying by mutableStateOf(false)
    var isMuted by mutableStateOf(false)
    var isPreviewerOpen by mutableStateOf(false)
    var currentTime by mutableLongStateOf(0L)
    var totalTime by mutableLongStateOf(0L)
    var bufferedPercentage by mutableIntStateOf(0)
    var enablePip by mutableStateOf(false)
    var isFullscreenMode by mutableStateOf(false)
    var speed by mutableFloatStateOf(1f)
    var isSpeedBoostActive by mutableStateOf(false)
    var controller: VideoPlayerController? = null
    var isSeeking = false

    fun initData(controller: VideoPlayerController) {
        this.controller = controller
        if (isMuted) {
            controller.setMuted(true)
        }
        controller.setPlaybackSpeed(speed)
    }

    fun changeSpeed(speed: Float) {
        this.speed = speed
        controller?.setPlaybackSpeed(speed)
    }

    fun startSpeedBoost() {
        if (!isSpeedBoostActive) {
            isSpeedBoostActive = true
            controller?.setPlaybackSpeed(2f)
        }
    }

    fun stopSpeedBoost() {
        if (isSpeedBoostActive) {
            isSpeedBoostActive = false
            controller?.setPlaybackSpeed(speed)
        }
    }

    fun seekTo(position: Long) {
        isSeeking = true
        currentTime = position
        controller?.seekTo(position)
        if (!isPlaying) {
            controller?.play()
        }
    }

    fun togglePlay() {
        if (isPlaying) {
            controller?.pause()
        } else {
            if (currentTime >= totalTime) {
                currentTime = 0
                controller?.seekTo(0)
            }
            controller?.play()
        }
    }

    fun toggleMute() {
        if (isMuted) {
            controller?.setMuted(false)
            isMuted = false
        } else {
            controller?.setMuted(true)
            isMuted = true
        }
    }

    fun updateTime() {
        if (isSeeking) return
        val ctrl = controller ?: return
        isPlaying = ctrl.isPlaying
        currentTime = ctrl.currentPosition.coerceAtLeast(0L)
        totalTime = ctrl.duration.coerceAtLeast(0L)
        bufferedPercentage = ctrl.bufferedPercentage
    }
}
