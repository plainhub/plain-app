package com.ismartcoding.plain.ui.components.mediaviewer.video

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.media3.exoplayer.ExoPlayer

/**
 * Manages audio focus for the video previewer using AUDIOFOCUS_GAIN_TRANSIENT.
 * This ensures the background music player receives AUDIOFOCUS_LOSS_TRANSIENT
 * (not the permanent AUDIOFOCUS_LOSS), so it auto-resumes when we release focus.
 */
class VideoAudioFocusManager(private val audioManager: AudioManager) {
    private var focusRequest: AudioFocusRequest? = null

    fun requestFocus(player: ExoPlayer) {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> player.pause()
                    AudioManager.AUDIOFOCUS_GAIN -> player.play()
                    AudioManager.AUDIOFOCUS_LOSS -> player.stop()
                }
            }
            .build()
        focusRequest = req
        audioManager.requestAudioFocus(req)
    }

    fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }
}
