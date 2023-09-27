package com.ismartcoding.lib.media

import android.media.AudioManager
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat

object AudioFocusHelper {
    fun createRequest(mediaPlayer: IMediaPlayer): AudioFocusRequestCompat {
        return AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        if (!mediaPlayer.isPlaying() && mediaPlayer.isPausedByTransientLossOfFocus) {
                            mediaPlayer.play()
                            mediaPlayer.isPausedByTransientLossOfFocus = false
                        }
                        mediaPlayer.setVolume(VOLUME_NORMAL)
                    }
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        // Lost focus for an unbounded amount of time: stop playback and release media playback
                        mediaPlayer.pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        // Lost focus for a short time, but we have to stop
                        // playback. We don't release the media playback because playback
                        // is likely to resume
                        val wasPlaying = mediaPlayer.isPlaying()
                        mediaPlayer.pause()
                        mediaPlayer.isPausedByTransientLossOfFocus = wasPlaying
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        // Lost focus for a short time, but it's ok to keep playing
                        // at an attenuated level
                        mediaPlayer.setVolume(VOLUME_DUCK)
                    }
                }
            }
            .setAudioAttributes(
                AudioAttributesCompat.Builder()
                    .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC).build(),
            ).build()
    }

    /**
     * The volume we set the media player to when we lose audio focus, but are
     * allowed to reduce the volume instead of stopping playback.
     */
    const val VOLUME_DUCK = 0.2f

    /** The volume we set the media player when we have audio focus.  */
    const val VOLUME_NORMAL = 1.0f
}
