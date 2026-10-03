package com.ismartcoding.plain.audio

import com.ismartcoding.plain.preferences.*
import com.ismartcoding.plain.appContext

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.ismartcoding.plain.lib.coMain
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.AudioAction
import com.ismartcoding.plain.features.audio.AudioCommands
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import com.ismartcoding.plain.events.AudioActionEvent
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.lib.sendEvent
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
        override fun onPlayerError(error: PlaybackException) {
            pendingStart = null
            player?.pause()
            refreshPlayingState()
            LogCat.e("Audio playback: ${error.message}")
            setChangedNotify(AudioAction.NOT_FOUND)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            LogCat.d("Player.isPlaying changed to: $isPlaying")
            refreshPlayingState()
            if (isPlaying) {
                val audio = pendingStart?.takeIf { it.first.path == currentPath }
                if (audio != null) {
                    pendingStart = null
                    coIO { AudioQueueManager.onStarted(audio.first, audio.second) }
                }
            }
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

    private var controller: MediaController? = null
    private var player: Player? = null
    private var pendingStart: Pair<DPlaylistAudio, Long>? = null
    val currentPath: String get() = player?.currentMediaItem?.mediaId.orEmpty()
    var playerProgress: Long = 0 // player progress in milliseconds
        get() {
            val currentPlayer = player
            if (currentPlayer == null) {
                return TempData.audioPlayPosition
            }

            val currentPositionMs = currentPlayer.currentPosition
            // Keep UI stable right after seek when player may briefly report 0.
            if (currentPositionMs == 0L && TempData.audioPlayPosition > 0L) {
                return TempData.audioPlayPosition
            }
            return currentPositionMs
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
            controller = mediaControllerFuture.get()
            player = checkNotNull(AudioPlayerService.nativePlayer).also {
                it.addListener(playerListener)
                refreshPlayingState()
                it.setPlaybackSpeed(UserPrefs.audioPlaybackSpeed.value)
            }
            coMain {
                callback()
            }
        }, MoreExecutors.directExecutor())
    }

    suspend fun load(context: Context, audio: DPlaylistAudio, positionMs: Long, speed: Float, revision: Long) {
        val ready = CompletableDeferred<Unit>()
        ensurePlayer(context) {
            try {
                TempData.audioPlayPosition = positionMs
                doPlay(audio, speed, revision)
                ready.complete(Unit)
            } catch (e: Exception) { ready.completeExceptionally(e) }
        }
        withTimeout(15_000) { ready.await() }
    }

    fun play() { coMain { player?.play() } }

    fun seekTo(positionMs: Long) {
        coMain {
            val seekPosition = positionMs
            TempData.audioPlayPosition = seekPosition
            val currentPlayer = player
            if (currentPlayer != null) {
                currentPlayer.seekTo(seekPosition)
                return@coMain
            }
        }
    }

    fun skipToNext() {
        skipTo(isNext = true)
    }

    fun skipToPrevious() {
        skipTo(isNext = false)
    }

    private fun skipTo(isNext: Boolean) {
        AudioCommands.submit(if (isNext) "NEXT" else "PREVIOUS")
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
            pendingStart = null
            player?.clearMediaItems()
            TempData.audioPlayPosition = 0
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        coMain {
            UserPrefs.audioPlaybackSpeed.value = speed
            player?.setPlaybackSpeed(speed)
        }
    }

    fun release() {
        player?.removeListener(playerListener)
        pendingStart = null
        player = null
        controller?.release()
        controller = null
        _isPlayingFlow.value = false
        TempData.audioPlayPosition = 0
    }

    private fun doPlay(
        audio: DPlaylistAudio,
        speed: Float,
        revision: Long,
    ) {
        pendingStart = audio to revision
        player?.setMediaItem(audio.toMediaItem())
        player?.prepare()
        player?.seekTo(TempData.audioPlayPosition)
        player?.setPlaybackSpeed(speed)
        player?.play()
    }

    fun setChangedNotify(action: AudioAction) {
        sendEvent(AudioActionEvent(action))
    }
}
