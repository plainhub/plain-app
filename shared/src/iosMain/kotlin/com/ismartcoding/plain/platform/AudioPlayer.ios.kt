@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.features.audio.AudioCommands
import kotlinx.coroutines.withContext
import com.ismartcoding.plain.features.audio.AudioQueueManager
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
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.AVPlayerTimeControlStatusPaused
import platform.Foundation.NSURL
import platform.Foundation.valueForKey
import platform.darwin.NSObject

actual fun createAudioPlayer(): AudioPlayer = AVPlayerAudioPlayer

private object AVPlayerAudioPlayer : AudioPlayer {
    private val _isPlayingFlow = MutableStateFlow(false)
    override val isPlayingFlow: StateFlow<Boolean> = _isPlayingFlow.asStateFlow()

    private var player: AVPlayer? = null
    private var currentAudio: DPlaylistAudio? = null
    private var pendingStart: Pair<DPlaylistAudio, Long>? = null
    override val currentPath: String get() = if (player == null) "" else currentAudio?.path.orEmpty()

    override suspend fun load(audio: DPlaylistAudio, positionMs: Long, speed: Float, revision: Long) = withContext(Dispatchers.Main) {
        currentAudio = audio
        TempData.audioPlayPosition = positionMs
        playInternal(audio, speed, revision)
    }

    /** Playback intent: true from play() until pause()/clear(); the flow
     *  mirrors it so play buttons stay steady across track switches. */
    private var playWhenReady = false
    private var pollJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override val progress: Long
        get() {
            val p = player ?: return TempData.audioPlayPosition
            return avPlayerTimeMs(p as NSObject, "currentTime")
        }

    override fun seekTo(positionMs: Long) {
        val ms = positionMs
        TempData.audioPlayPosition = ms
        scope.launch {
            val p = player ?: return@launch
            avPlayerSeekToMs(p as NSObject, ms)
        }
    }

    override fun pause() {
        scope.launch {
            playWhenReady = false
            val p = player ?: return@launch
            TempData.audioPlayPosition = avPlayerTimeMs(p as NSObject, "currentTime")
            avPlayerPerform(p as NSObject, "pause")
            _isPlayingFlow.value = false
            stopPolling()
        }
    }

    override fun play() {
        scope.launch {
            playWhenReady = true
            val p = player
            if (p != null) {
                avPlayerPerform(p as NSObject, "play")
                avPlayerSetRate(p as NSObject, UserPrefs.audioPlaybackSpeed.value)
                _isPlayingFlow.value = true
                startPolling()
                return@launch
            }

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

    override fun clear() {
        scope.launch {
            playWhenReady = false
            val p = player
            if (p != null) {
                avPlayerPerform(p as NSObject, "pause")
                avPlayerPerformWithArg(p as NSObject, "replaceCurrentItemWithPlayerItem:", null)
            }
            player = null
            currentAudio = null
            pendingStart = null
            _isPlayingFlow.value = false
            TempData.audioPlayPosition = 0
            stopPolling()
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        UserPrefs.audioPlaybackSpeed.value = speed
        scope.launch {
            val p = player ?: return@launch
            if (playWhenReady) avPlayerSetRate(p as NSObject, speed)
        }
    }

    private fun playInternal(audio: DPlaylistAudio, speed: Float, revision: Long) {
        try {
            configureSession()
            val url = NSURL.fileURLWithPath(audio.path)
            val item = AVPlayerItem(uRL = url)
            val seekMs = TempData.audioPlayPosition
            pendingStart = audio to revision
            val existing = player
            if (existing != null) {
                avPlayerPerformWithArg(existing as NSObject, "replaceCurrentItemWithPlayerItem:", item)
                avPlayerSeekToMs(existing as NSObject, seekMs)
                avPlayerPerform(existing as NSObject, "play")
                avPlayerSetRate(existing as NSObject, speed)
            } else {
                val newPlayer = AVPlayer(uRL = url)
                seekPlayerIfNeeded(newPlayer as NSObject, seekMs)
                avPlayerPerform(newPlayer as NSObject, "play")
                avPlayerSetRate(newPlayer as NSObject, speed)
                player = newPlayer
            }
            playWhenReady = true
            _isPlayingFlow.value = true
            startPolling()

        } catch (e: Exception) {
            pendingStart = null
            playWhenReady = false
            _isPlayingFlow.value = false
            throw e
        }
    }

    private fun configureSession() {
        check(AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, null)) { "Audio session configuration failed" }
    }

    private fun startPolling() {
        stopPolling()
        pollJob = scope.launch {
            while (isActive) {
                delay(200)
                val p = player ?: break
                val position = avPlayerTimeMs(p as NSObject, "currentTime")
                TempData.audioPlayPosition = position
                val status = (p.valueForKey("timeControlStatus") as? platform.Foundation.NSNumber)?.intValue()
                if (status?.toLong() == AVPlayerTimeControlStatusPlaying) {
                    pendingStart?.let { audio ->
                        pendingStart = null
                        scope.launch { AudioQueueManager.onStarted(audio.first, audio.second) }
                    }
                }
                val item = avPlayerCurrentItem(p as NSObject)
                val duration = item?.let { avPlayerTimeMs(it, "duration") } ?: 0L
                if (playWhenReady && status?.toLong() == AVPlayerTimeControlStatusPaused && duration > 0 && position >= duration - 50) {
                    onCompleted()
                    break
                }
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun onCompleted() { AudioCommands.submit("COMPLETED") }

}

private fun seekPlayerIfNeeded(target: NSObject, ms: Long) {
    if (ms > 0) avPlayerSeekToMs(target, ms)
}
