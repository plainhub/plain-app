package com.ismartcoding.plain.features.audio

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.ismartcoding.lib.channel.sendEvent
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
    var playerProgress: Long = 0 // player progress in milliseconds
        get() {
            return if (player?.isPlaying == true) {
                player?.currentPosition ?: 0
            } else {
                field
            }
        }

    var pendingQuit: Boolean = false

    fun setRepeatMode() {
        when (TempData.audioPlayMode) {
            MediaPlayMode.REPEAT -> player?.repeatMode = Player.REPEAT_MODE_ALL
            MediaPlayMode.REPEAT_ONE -> player?.repeatMode = Player.REPEAT_MODE_ONE
            MediaPlayMode.SHUFFLE -> player?.repeatMode = Player.REPEAT_MODE_ALL
        }
        player?.shuffleModeEnabled = TempData.audioPlayMode == MediaPlayMode.SHUFFLE
    }

    private fun ensurePlayer(context: Context, callback: () -> Unit) {
        val sessionToken = SessionToken(context, ComponentName(context, AudioPlayerService::class.java))
        val mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        mediaControllerFuture.addListener({
            player = mediaControllerFuture.get()
            setRepeatMode()
            callback()
        }, MoreExecutors.directExecutor())
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
            if (player?.availableCommands?.contains(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) == true) {
                player?.pause()
                player?.seekTo(playerProgress)
                player?.prepare()
                player?.play()
            }
        } else {
            play()
        }
    }

    fun skipToNext() {
        playerProgress = 0
        player?.seekToNext()
    }

    fun skipToPrevious() {
        playerProgress = 0
        player?.seekToPrevious()
    }

    fun pause() {
        playerProgress = player?.currentPosition ?: 0
        if (player?.isPlaying == true) {
            player?.pause()
        }
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
            player?.seekTo(index, playerProgress)
            player?.prepare()
            player?.play()
        }
    }

    fun setChangedNotify(action: AudioAction) {
        sendEvent(AudioActionEvent(action))
    }
}
