package com.ismartcoding.plain.features

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.Player
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
import com.ismartcoding.plain.data.DPlaylistAudio
import com.ismartcoding.plain.enums.AudioAction
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.preference.AudioPlayingPreference
import com.ismartcoding.plain.preference.AudioPlaylistPreference
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

    fun ensurePlayer(context: Context, callback: () -> Unit = {}) {
        if (player != null) {
            callback()
            return
        }
        val sessionToken = SessionToken(context, ComponentName(context, AudioPlayerService::class.java))
        val mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        mediaControllerFuture.addListener({
            player = mediaControllerFuture.get()
            callback()
        }, MoreExecutors.directExecutor())
    }

    fun play(
        context: Context,
        playlistAudio: DPlaylistAudio
    ) {
        coMain {
            playerProgress = 0
            withIO { AudioPlaylistPreference.addAsync(context, listOf(playlistAudio)) }
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
            playerProgress = 0
            ensurePlayer(context) {
                doPlay(playlistAudio)
            }
        }
    }

    fun play() {
        coMain {
            val current = player?.currentMediaItem
            if (current != null) {
                player?.seekTo(playerProgress)
                player?.play()
                return@coMain
            }

            val context = MainApp.instance
            val playlistAudio = ensureCurrentPlaylistAudio()
            try {
                if (playlistAudio != null) {
                    ensurePlayer(context) {
                        doPlay(playlistAudio)
                    }
                }
            } catch (e: Exception) {
                LogCat.e(e.toString())
                if (playlistAudio != null) {
                    withIO { AudioPlaylistPreference.deleteAsync(context, setOf(playlistAudio.path)) }
                }
                setChangedNotify(AudioAction.NOT_FOUND)
            }
        }
    }

    private suspend fun ensureCurrentPlaylistAudio(): DPlaylistAudio? {
        val context = MainApp.instance
        val path = withIO { AudioPlayingPreference.getValueAsync(context) }
        if (path.isEmpty()) {
            return null
        }
        val playlistAudio = withIO { DPlaylistAudio.fromPath(context, path) }
        withIO { AudioPlaylistPreference.addAsync(context, listOf(playlistAudio)) }
        return playlistAudio
    }

    fun seekTo(progress: Long) {
        playerProgress = progress * 1000
        if (player?.isPlaying == true) {
            player?.pause()
            player?.seekTo(playerProgress)
            player?.prepare()
            player?.play()
        } else {
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
        val context = MainApp.instance
        coIO {
            var audio: DPlaylistAudio
            var playerAudioList = AudioPlaylistPreference.getValueAsync(context)
            val playingPath = AudioPlayingPreference.getValueAsync(context)
            if (playerAudioList.isEmpty()) {
                if (playingPath.isNotEmpty()) {
                    audio = DPlaylistAudio.fromPath(context, playingPath)
                    AudioPlaylistPreference.addAsync(context, listOf(audio))
                    playerAudioList = listOf(audio)
                } else {
                    return@coIO
                }
            }

            if (TempData.audioPlayMode == MediaPlayMode.SHUFFLE) {
                audio = playerAudioList.random()
            } else {
                if (playingPath.isNotEmpty()) {
                    var index = playerAudioList.indexOfFirst { it.path == playingPath }
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
                    audio = playerAudioList[index]
                } else {
                    audio = playerAudioList[if (isNext) 0 else (playerAudioList.size - 1)]
                }
            }

            LogCat.d("skipTo: ${audio.path}")
            playerProgress = 0
            coMain {
                ensurePlayer(context) {
                    doPlay(audio)
                }
            }
        }
    }

    fun pause() {
        playerProgress = player?.currentPosition ?: 0
        if (player?.isPlaying == true) {
            player?.pause()
        }
    }

    fun clear() {
        if (player?.isPlaying == true) {
            player?.pause()
        }
        player?.clearMediaItems()
    }

    fun release() {
        player = null
    }

    private fun doPlay(
        audio: DPlaylistAudio,
    ) {
        pendingQuit = false
        player?.setMediaItem(audio.toMediaItem())
        player?.prepare()
        player?.seekTo(playerProgress)
        player?.play()
    }

    fun repeatCurrent() {
        playerProgress = 0
        player?.seekTo(playerProgress)
        player?.play()
    }

    fun setChangedNotify(action: AudioAction) {
        sendEvent(AudioActionEvent(action))
    }
}
