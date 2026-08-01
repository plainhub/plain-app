@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.Foundation.NSURL
import platform.darwin.NSObject

actual fun createAudioPlayer(): AudioPlayer = AVPlayerAudioPlayer

private object AVPlayerAudioPlayer : AudioPlayer {
    private val _isPlayingFlow = MutableStateFlow(false)
    override val isPlayingFlow: StateFlow<Boolean> = _isPlayingFlow.asStateFlow()

    private var player: AVPlayer? = null
    private val playlist = mutableListOf<DPlaylistAudio>()
    private var currentIndex = -1
    private var currentAudio: DPlaylistAudio? = null
    private var pollJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override val progress: Long
        get() {
            val p = player ?: return TempData.audioPlayPosition
            return avPlayerTimeMs(p as NSObject, "currentTime")
        }

    override fun seekTo(progress: Long) {
        val ms = progress * 1000
        TempData.audioPlayPosition = ms
        scope.launch {
            val p = player ?: return@launch
            avPlayerSeekToMs(p as NSObject, ms)
        }
    }

    override fun pause() {
        scope.launch {
            val p = player ?: return@launch
            TempData.audioPlayPosition = avPlayerTimeMs(p as NSObject, "currentTime")
            avPlayerPerform(p as NSObject, "pause")
            _isPlayingFlow.value = false
            stopPolling()
        }
    }

    override fun play() {
        scope.launch {
            val p = player
            if (p != null) {
                avPlayerPerform(p as NSObject, "play")
                _isPlayingFlow.value = true
                startPolling()
                return@launch
            }
            val audio = currentAudio ?: return@launch
            playInternal(audio)
        }
    }

    override fun restartIfPlaying() {
        scope.launch {
            val p = player ?: return@launch
            if (avPlayerRate(p as NSObject) > 0f) {
                avPlayerPerform(p as NSObject, "pause")
                avPlayerPerform(p as NSObject, "play")
            }
        }
    }

    override fun playFromPath(path: String) {
        scope.launch {
            val audio = playlistAudioFromPath(path)
            playlist.clear()
            playlist.add(audio)
            currentIndex = 0
            currentAudio = audio
            TempData.audioPlayPosition = 0
            playInternal(audio)
        }
    }

    override fun justPlay(audio: DPlaylistAudio) {
        scope.launch {
            val idx = playlist.indexOfFirst { it.path == audio.path }
            if (idx >= 0) currentIndex = idx
            currentAudio = audio
            TempData.audioPlayPosition = 0
            playInternal(audio)
        }
    }

    override fun clear() {
        scope.launch {
            val p = player
            if (p != null) {
                avPlayerPerform(p as NSObject, "pause")
                avPlayerPerformWithArg(p as NSObject, "replaceCurrentItemWithPlayerItem:", null)
            }
            player = null
            playlist.clear()
            currentIndex = -1
            currentAudio = null
            _isPlayingFlow.value = false
            TempData.audioPlayPosition = 0
            stopPolling()
        }
    }

    override fun skipToPrevious() = skipTo(isNext = false)

    override fun skipToNext() = skipTo(isNext = true)

    private fun skipTo(isNext: Boolean) {
        scope.launch {
            if (playlist.isEmpty()) return@launch
            val audio = when (TempData.audioPlayMode.value) {
                MediaPlayMode.SHUFFLE -> playlist.random()
                else -> {
                    var index = if (currentIndex < 0) 0 else currentIndex
                    index = if (isNext) {
                        (index + 1) % playlist.size
                    } else {
                        (index - 1 + playlist.size) % playlist.size
                    }
                    currentIndex = index
                    playlist[index]
                }
            }
            currentAudio = audio
            TempData.audioPlayPosition = 0
            playInternal(audio)
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        TempData.audioPlaybackSpeed.value = speed
        scope.launch {
            val p = player ?: return@launch
            avPlayerSetRate(p as NSObject, speed)
        }
    }

    private fun playInternal(audio: DPlaylistAudio) {
        try {
            configureSession()
            val url = NSURL.fileURLWithPath(audio.path)
            val item = AVPlayerItem(uRL = url)
            val seekMs = TempData.audioPlayPosition
            val speed = TempData.audioPlaybackSpeed.value
            val existing = player
            if (existing != null) {
                avPlayerPerformWithArg(existing as NSObject, "replaceCurrentItemWithPlayerItem:", item)
                avPlayerSeekToMs(existing as NSObject, seekMs)
                avPlayerSetRate(existing as NSObject, speed)
                avPlayerPerform(existing as NSObject, "play")
            } else {
                val newPlayer = AVPlayer(uRL = url)
                seekPlayerIfNeeded(newPlayer as NSObject, seekMs)
                avPlayerSetRate(newPlayer as NSObject, speed)
                avPlayerPerform(newPlayer as NSObject, "play")
                player = newPlayer
            }
            _isPlayingFlow.value = true
            startPolling()
        } catch (e: Exception) {
            LogCat.e("playInternal: ${e.message}")
        }
    }

    private fun configureSession() {
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, null)
        } catch (e: Exception) {
            LogCat.e("configureSession: ${e.message}")
        }
    }

    private fun startPolling() {
        stopPolling()
        pollJob = scope.launch {
            while (isActive) {
                delay(200)
                val p = player ?: break
                if (avPlayerRate(p as NSObject) == 0f) {
                    _isPlayingFlow.value = false
                    TempData.audioPlayPosition = 0
                    onCompleted()
                    break
                }
                TempData.audioPlayPosition = avPlayerTimeMs(p as NSObject, "currentTime")
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun onCompleted() {
        when (TempData.audioPlayMode.value) {
            MediaPlayMode.REPEAT_ONE -> {
                val audio = currentAudio
                if (audio != null) {
                    TempData.audioPlayPosition = 0
                    playInternal(audio)
                }
            }
            else -> skipTo(isNext = true)
        }
    }
}

private fun seekPlayerIfNeeded(target: NSObject, ms: Long) {
    if (ms > 0) avPlayerSeekToMs(target, ms)
}
