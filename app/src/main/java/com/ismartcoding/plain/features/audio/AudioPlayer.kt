package com.ismartcoding.plain.features.audio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.preference.AudioPlayingPreference
import com.ismartcoding.plain.data.preference.AudioPlaylistPreference
import com.ismartcoding.plain.features.AudioActionEvent
import com.ismartcoding.plain.services.AudioPlayerService

object AudioPlayer {
    fun isPlaying(): Boolean {
        return player?.isPlaying == true
    }

    private var player: Player? = null
    private var playerProgress: Long = 0 // player progress
    var ignoreStateEnd = false
    var pendingQuit: Boolean = false

    private var listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            LogCat.d("onMediaItemTransition: ${mediaItem?.mediaId}, $reason")
            coIO {
                val context = MainApp.instance
                if (mediaItem == null) {
                    AudioPlayingPreference.putAsync(context, "")
                    return@coIO
                }
                AudioPlayingPreference.putAsync(context, mediaItem.mediaId)
                setChangedNotify(AudioAction.PLAY)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            LogCat.d("onPlaybackStateChanged: $playbackState")
            when (playbackState) {
                Player.STATE_READY -> {
                    ignoreStateEnd = false
                    LogCat.d("Player: STATE_READY, ${player?.currentPosition}")
                    if (player?.isPlaying == true) {
                        setChangedNotify(AudioAction.PLAY)
                    } else {
                        setChangedNotify(AudioAction.PAUSE)
                    }
                }

                Player.STATE_ENDED -> {
                    LogCat.d("Player: STATE_ENDED, ignoreStateEnd: $ignoreStateEnd")
                    if (ignoreStateEnd) {
                        return
                    }
                    if (pendingQuit) {
                        pendingQuit = false
                        pause()
                        return
                    }
                }

                Player.STATE_IDLE -> {
                    LogCat.d("Player: STATE_IDLE")
                    setChangedNotify(AudioAction.PAUSE)
                }
            }
        }
    }

    fun setRepeatMode() {
        when (TempData.audioPlayMode) {
            MediaPlayMode.REPEAT -> player?.repeatMode = Player.REPEAT_MODE_ALL
            MediaPlayMode.REPEAT_ONE -> player?.repeatMode = Player.REPEAT_MODE_ONE
            MediaPlayMode.SHUFFLE -> player?.repeatMode = Player.REPEAT_MODE_ALL
        }
        player?.shuffleModeEnabled = TempData.audioPlayMode == MediaPlayMode.SHUFFLE
    }

    private fun ensurePlayer(context: Context, callback: () -> Unit = {}) {
        val sessionToken = SessionToken(context, ComponentName(context, AudioPlayerService::class.java))
        val mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        mediaControllerFuture.addListener({
            player = mediaControllerFuture.get()
            setRepeatMode()
            player?.addListener(listener)
            callback()
        }, MoreExecutors.directExecutor())
    }

    fun getPlayerProgress(): Long {
        return if (player?.isPlaying == true) {
            (player?.currentPosition ?: 0) / 1000
        } else {
            playerProgress / 1000
        }
    }

    fun play(
        context: Context,
        playlistAudio: DPlaylistAudio
    ) {
        coMain {
            playerProgress = 0
            val playerAudioList = withIO { AudioPlaylistPreference.addAsync(context, listOf(playlistAudio)) }
            withIO { AudioPlayingPreference.putAsync(context, playlistAudio.path) }
            if (player == null) {
                ensurePlayer(context) {
                    doPlay(playerAudioList.map { it.path }, playlistAudio.path)
                }
            } else {
                doPlay(playerAudioList.map { it.path }, playlistAudio.path)
            }
        }
    }

    fun play() {
        coMain {
            val context = MainApp.instance
            val path = withIO { AudioPlayingPreference.getValueAsync(context) }
            if (path.isEmpty()) {
                return@coMain
            }
            try {
                val playlistAudio = withIO { DPlaylistAudio.fromPath(context, path) }
                val playerAudioList = withIO { AudioPlaylistPreference.addAsync(context, listOf(playlistAudio)) }
                if (player == null) {
                    ensurePlayer(context) {
                        doPlay(playerAudioList.map { it.path }, path)
                    }
                } else {
                    doPlay(playerAudioList.map { it.path }, path)
                }
            } catch (e: Exception) {
                LogCat.e(e.toString())
                AudioPlaylistPreference.deleteAsync(context, setOf(path))
                setChangedNotify(AudioAction.NOT_FOUND)
            }
        }
    }

    fun seekTo(progress: Long) {
        playerProgress = progress * 1000
        if (player?.isPlaying == true) {
            player?.seekTo(playerProgress)
        } else {
            play()
        }
        setChangedNotify(AudioAction.SEEK)
    }

    fun skipToNext() {
        player?.seekToNext()
        //skipTo(isNext = true)
    }

    fun skipToPrevious() {
        player?.seekToPrevious()
        skipTo(isNext = false)
    }

    private fun skipTo(isNext: Boolean) {
        val context = MainApp.instance
        coMain {
            var path = ""
            var playerAudioList = withIO { AudioPlaylistPreference.getValueAsync(context) }
            if (playerAudioList.isEmpty()) {
                path = withIO { AudioPlayingPreference.getValueAsync(context) }
                if (path.isNotEmpty()) {
                    val item = withIO { DPlaylistAudio.fromPath(context, path) }
                    playerAudioList = arrayListOf(item)
                    AudioPlaylistPreference.putAsync(context, playerAudioList)
                } else {
                    return@coMain
                }
            }

            if (TempData.audioPlayMode == MediaPlayMode.SHUFFLE) {
                path = playerAudioList.random().path
            } else {
                path = withIO { AudioPlayingPreference.getValueAsync(context) }
                if (path.isNotEmpty()) {
                    var index = playerAudioList.indexOfFirst { it.path == path }
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
                    path = playerAudioList[index].path
                } else {
                    path = playerAudioList[if (isNext) 0 else (playerAudioList.size - 1)].path
                }
            }

            withIO { AudioPlayingPreference.putAsync(context, path) }
            playerProgress = 0
            doPlay(playerAudioList.map { it.path }, path)
        }
    }

    fun pause() {
        playerProgress = player?.currentPosition ?: 0

        if (player?.isPlaying == true) {
            player?.pause()
        }

        setChangedNotify(AudioAction.PAUSE)
    }

    fun showNotification() {
        setChangedNotify(AudioAction.NOTIFICATION)
    }

    fun release() {
        player = null
    }

    private fun doPlay(
        paths: List<String>,
        path: String,
    ) {
        pendingQuit = false
        player?.clearMediaItems()
        paths.forEach {
            player?.addMediaItem(
                MediaItem.Builder()
                    .setMediaId(it)
                    .build()
            )
        }
        val index = paths.indexOf(path)
        LogCat.d("doPlay: ${path}, $index, $playerProgress")
        if (index != -1) {
            ignoreStateEnd = true
            player?.seekTo(index, playerProgress)
            player?.prepare()
            player?.play()
        }
    }

    private fun setChangedNotify(action: AudioAction) {
        sendEvent(AudioActionEvent(action))
    }
}
