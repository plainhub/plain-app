package com.ismartcoding.plain.features.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.media.AudioManagerCompat
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.pathToUri
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.lib.media.AudioFocusHelper
import com.ismartcoding.lib.media.IMediaPlayer
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.data.preference.AudioPlayModePreference
import com.ismartcoding.plain.data.preference.AudioPlayingPreference
import com.ismartcoding.plain.data.preference.AudioPlaylistPreference
import com.ismartcoding.plain.features.AudioActionEvent

class AudioPlayer : IMediaPlayer {
    companion object {
        val instance = AudioPlayer()
    }

    override fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    private var mediaPlayer: MediaPlayer? = null
    private val audioManager by lazy { MainApp.instance.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    override var isPausedByTransientLossOfFocus = false
    private val audioFocusRequest = AudioFocusHelper.createRequest(this)
    private var playerProgress: Int = 0 // player progress

    var pendingQuit: Boolean = false

    fun setPlayerProgress(progress: Int) {
        playerProgress = progress * 1000
    }

    fun getPlayerProgress(): Int {
        return if (mediaPlayer?.isPlaying == true) {
            (mediaPlayer?.currentPosition ?: 0) / 1000
        } else {
            playerProgress / 1000
        }
    }

    fun play(path: String) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            setListen()
        }
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
        }
        mediaPlayer?.reset()
        mediaPlayer?.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        mediaPlayer?.setDataSource(MainApp.instance, path.pathToUri())
        mediaPlayer?.prepareAsync()
        requestFocus()
    }

    override fun play() {
        coIO {
            val context = MainApp.instance
            val playing = AudioPlayingPreference.getValueAsync(context) ?: return@coIO
            try {
                play(playing.path)
            } catch (e: Exception) {
                LogCat.e(e.toString())
                AudioPlaylistPreference.deleteAsync(context, setOf(playing.path))
                setChangedNotify(AudioAction.NOT_FOUND)
            }
        }
    }

    fun seekTo(progress: Int) {
        if (mediaPlayer?.isPlaying == true) {
            playerProgress = progress * 1000
            mediaPlayer?.seekTo(playerProgress)
        } else {
            setPlayerProgress(progress)
            play()
        }
        setChangedNotify(AudioAction.SEEK)
    }

    fun skipToNext() {
        skipTo(isNext = true)
    }

    fun skipToPrevious() {
        skipTo(isNext = false)
    }

    private fun skipTo(isNext: Boolean) {
        val context = MainApp.instance
        coIO {
            var playerAudioList = AudioPlaylistPreference.getValueAsync(context)
            if (playerAudioList.isEmpty()) {
                val playing = AudioPlayingPreference.getValueAsync(context)
                if (playing != null) {
                    AudioPlaylistPreference.addAsync(context, listOf(playing))
                    playerAudioList = AudioPlaylistPreference.getValueAsync(context)
                } else {
                    return@coIO
                }
            }

            if (AudioPlayModePreference.getValueAsync(context) == MediaPlayMode.SHUFFLE) {
                AudioPlayingPreference.putAsync(context, playerAudioList.random())
            } else {
                val playing = AudioPlayingPreference.getValueAsync(context)
                if (playing != null) {
                    var index = playerAudioList.indexOfFirst { it.path == playing.path }
                    if (isNext) {
                        index++
                        if (index > playerAudioList.size - 1) {
                            index = 0
                        }
                    } else {
                        index--
                        if (index < 0) {
                            index = playerAudioList.size - 1
                        }
                    }
                    AudioPlayingPreference.putAsync(context, playerAudioList[index])
                } else {
                    AudioPlayingPreference.putAsync(context, playerAudioList[if (isNext) 0 else (playerAudioList.size - 1)])
                }
            }

            playerProgress = 0
            play()
        }
    }

    override fun pause() {
        playerProgress = mediaPlayer?.currentPosition ?: 0

        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }

        setChangedNotify(AudioAction.PAUSE)
    }

    fun showNotification() {
        setChangedNotify(AudioAction.NOTIFICATION)
    }

    override fun stop() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            abandonFocus()
        }
    }

    override fun setVolume(volume: Float) {
        mediaPlayer?.setVolume(volume, volume)
    }

    private fun setListen() {
        mediaPlayer?.setOnPreparedListener {
            mediaPlayer?.seekTo(playerProgress)
            coIO {
                mediaPlayer?.start()
            }
            setChangedNotify(AudioAction.PLAY)
        }

        mediaPlayer?.setOnCompletionListener {
            setChangedNotify(AudioAction.COMPLETE)
        }

        mediaPlayer?.setOnErrorListener { mp, what, extra ->
            LogCat.e("MediaPlayer error type:$what, code:$extra, currentPosition:${mp.currentPosition}")
            return@setOnErrorListener false
        }
    }

    private fun setChangedNotify(action: AudioAction) {
        sendEvent(AudioActionEvent(action))
    }

    private fun requestFocus(): Boolean {
        return AudioManagerCompat.requestAudioFocus(
            audioManager,
            audioFocusRequest,
        ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        LogCat.e("abandonFocus")
        AudioManagerCompat.abandonAudioFocusRequest(audioManager, audioFocusRequest)
    }
}
