package com.ismartcoding.plain.audio
import com.ismartcoding.plain.appContext

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.ismartcoding.plain.lib.coMain
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.AudioAction
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.events.AudioActionEvent
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.preferences.AudioPlayingPreference
import com.ismartcoding.plain.services.AudioPlayerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AudioPlayer {
    private val _isPlayingFlow = MutableStateFlow(false)
    val isPlayingFlow: StateFlow<Boolean> = _isPlayingFlow.asStateFlow()

    fun isPlaying(): Boolean {
        return player?.isPlaying == true
    }

    /**
     * The flow exposes *playback intent* (playWhenReady), not "audio is coming
     * out right now": it stays true while the player switches tracks, so play
     * buttons never flicker during a skip. False only when paused, stopped or
     * uninitialized. STATE_ENDED keeps playWhenReady true — skipTo pauses the
     * player explicitly when the queue runs dry.
     */
    private fun refreshPlayingState() {
        val p = player ?: return
        _isPlayingFlow.value = p.playWhenReady && p.playbackState != Player.STATE_IDLE
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            LogCat.d("Player.isPlaying changed to: $isPlaying")
            refreshPlayingState()
            if (!isPlaying && player != null) {
                TempData.audioPlayPosition = player?.currentPosition ?: 0
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            refreshPlayingState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            refreshPlayingState()
        }
    }

    private var player: Player? = null
    var playerProgress: Long = 0 // player progress in milliseconds
        get() {
            val currentPlayer = player
            if (currentPlayer == null) {
                return TempData.audioPlayPosition
            }

            val currentPosition = currentPlayer.currentPosition
            // Keep UI stable right after seek when player may briefly report 0.
            if (currentPosition == 0L && TempData.audioPlayPosition > 0L) {
                return TempData.audioPlayPosition
            }
            return currentPosition
        }

    fun ensurePlayer(context: Context, callback: suspend () -> Unit = {}) {
        if (player != null) {
            coMain {
                callback()
            }
            return
        }
        val sessionToken = SessionToken(context, ComponentName(context, AudioPlayerService::class.java))
        val mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        mediaControllerFuture.addListener({
            player = mediaControllerFuture.get().also {
                it.addListener(playerListener)
                refreshPlayingState()
                it.setPlaybackSpeed(TempData.audioPlaybackSpeed.value)
            }
            coMain {
                callback()
            }
        }, MoreExecutors.directExecutor())
    }

    fun play(
        context: Context,
        playlistAudio: DPlaylistAudio
    ) {
        coMain {
            TempData.audioPlayPosition = 0
            AudioQueueManager.enqueue(listOf(playlistAudio))
            ensurePlayer(context) {
                doPlay(playlistAudio)
            }
        }
    }

    fun justPlay(
        context: Context,
        playlistAudio: DPlaylistAudio
    ) {
        coMain {
            TempData.audioPlayPosition = 0
            ensurePlayer(context) {
                doPlay(playlistAudio)
            }
        }
    }

    fun play() {
        coMain {
            val current = player?.currentMediaItem
            if (current != null) {
                player?.seekTo(TempData.audioPlayPosition)
                player?.play()
                return@coMain
            }

            val context = appContext
            val path = AudioPlayingPreference.getValueAsync()
            if (path.isEmpty()) {
                return@coMain
            }
            val playlistAudio = try {
                DPlaylistAudio.fromPath(context, path)
            } catch (e: Exception) {
                LogCat.e(e.toString())
                null
            }
            if (playlistAudio != null) {
                try {
                    ensurePlayer(context) {
                        doPlay(playlistAudio)
                    }
                } catch (e: Exception) {
                    LogCat.e(e.toString())
                    setChangedNotify(AudioAction.NOT_FOUND)
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        coMain {
            val seekPosition = positionMs
            TempData.audioPlayPosition = seekPosition
            val currentPlayer = player
            if (currentPlayer != null) {
                currentPlayer.seekTo(seekPosition)
                return@coMain
            }
            play()
        }
    }

    fun skipToNext() {
        skipTo(isNext = true)
    }

    fun skipToPrevious() {
        skipTo(isNext = false)
    }

    private fun skipTo(isNext: Boolean) {
        val context = appContext
        coIO {
            val audio = AudioQueueManager.resolveNext(
                isNext = isNext,
                shuffle = TempData.audioPlayMode.value == MediaPlayMode.SHUFFLE,
            )
            if (audio == null) {
                LogCat.d("skipTo: nothing to play, queue is empty")
                // Stop so playWhenReady (and the flow) turns off instead of
                // pretending to play a finished queue.
                coMain { player?.pause() }
                return@coIO
            }
            LogCat.d("skipTo: ${audio.path}")
            coMain {
                ensurePlayer(context) {
                    TempData.audioPlayPosition = 0
                    doPlay(audio)
                }
            }
        }
    }

    fun pause() {
        coMain {
            TempData.audioPlayPosition = player?.currentPosition ?: 0
            player?.pause()
        }
    }

    fun clear() {
        coMain {
            if (player?.isPlaying == true) {
                player?.pause()
            }
            player?.clearMediaItems()
            TempData.audioPlayPosition = 0
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        coMain {
            TempData.audioPlaybackSpeed.value = speed
            player?.setPlaybackSpeed(speed)
        }
    }

    fun release() {
        player?.removeListener(playerListener)
        player = null
        _isPlayingFlow.value = false
        TempData.audioPlayPosition = 0
    }

    private fun doPlay(
        audio: DPlaylistAudio,
    ) {
        player?.setMediaItem(audio.toMediaItem())
        player?.prepare()
        player?.seekTo(TempData.audioPlayPosition)
        player?.setPlaybackSpeed(TempData.audioPlaybackSpeed.value)
        player?.play()
        coIO { AudioQueueManager.onPlaying(audio.path, audio.title, audio.artist, audio.durationMs) }
    }

    fun setChangedNotify(action: AudioAction) {
        sendEvent(AudioActionEvent(action))
    }
}
