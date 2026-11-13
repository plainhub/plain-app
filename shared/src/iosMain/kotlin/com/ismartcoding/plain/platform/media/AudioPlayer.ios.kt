@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform.media

import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.rust.host.audio.AudioEngineHost
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.currentItem
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.currentTime
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.rate
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.timeControlStatus
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL

actual fun checkAudioPlaybackPermission(onGranted: () -> Unit) = onGranted()

actual fun createAudioPlayer(): AudioPlayer = AVPlayerAudioPlayer

private object AVPlayerAudioPlayer : AudioPlayer {
    private val _isPlayingFlow = MutableStateFlow(false)
    override val isPlayingFlow: StateFlow<Boolean> = _isPlayingFlow.asStateFlow()

    private var player: AVPlayer? = null
    private var currentAudio: AudioItem? = null
    private var pendingStart: Pair<AudioItem, Long>? = null
    private var activeRevision = -1L
    private var lastPositionMs = 0L
    private var completionObserver: Any? = null
    private var playbackSpeed = 1f

    override fun bindRevision(revision: Long) {
        activeRevision = revision
        observeCompletion()
    }

    private fun observeCompletion() {
        completionObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        completionObserver = null
        val item = player?.currentItem ?: return
        val revision = activeRevision
        val path = currentAudio?.path ?: return
        completionObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            AVPlayerItemDidPlayToEndTimeNotification, `object` = item, queue = NSOperationQueue.mainQueue,
        ) {
            if (activeRevision == revision && player?.currentItem == item) {
                flushPendingStart()
                stopObservingPlayback()
                AudioEngineHost.reportCompleted(path, revision, progress)
            }
        }
    }
    override val currentPath: String get() = if (player == null) "" else currentAudio?.path.orEmpty()

    override suspend fun load(audio: AudioItem, positionMs: Long, speed: Float, revision: Long) = withContext(Dispatchers.Main) {
        AudioEngineHost.stopProgressReporting()
        stopObservingPlayback()
        lastPositionMs = positionMs
        playInternal(audio, positionMs, speed, revision)
    }

    private var playWhenReady = false
    private var playbackObserver: Pair<AVPlayer, Any>? = null

    override val progress: Long
        get() {
            val p = player ?: return lastPositionMs
            return mediaTimeMs(p.currentTime())
        }

    override suspend fun seekTo(positionMs: Long) = withContext(Dispatchers.Main) {
        lastPositionMs = positionMs
        player?.let { it.seekToTime(CMTimeMake(positionMs, 1000)) }
        Unit
    }

    override suspend fun pause() = withContext(Dispatchers.Main) {
        if (player?.timeControlStatus == AVPlayerTimeControlStatusPlaying) flushPendingStart()
        AudioEngineHost.stopProgressReporting(currentPath, activeRevision, progress)
        playWhenReady = false
        player?.let {
            lastPositionMs = mediaTimeMs(it.currentTime())
            it.pause()
        }
        _isPlayingFlow.value = false
        stopObservingPlayback()
    }

    override suspend fun play() = withContext(Dispatchers.Main) {
        playWhenReady = true
        player?.let {
            observePlayback()
            it.play()
            it.rate = playbackSpeed
            _isPlayingFlow.value = true
        }
        Unit
    }

    override suspend fun restartIfPlaying() = withContext(Dispatchers.Main) {
        player?.let {
            if (it.rate > 0f) {
                it.pause()
                it.play()
                it.rate = playbackSpeed
            }
        }
        Unit
    }

    override suspend fun clear() = withContext(Dispatchers.Main) {
        if (player?.timeControlStatus == AVPlayerTimeControlStatusPlaying) flushPendingStart()
        AudioEngineHost.stopProgressReporting(currentPath, activeRevision, progress)
        playWhenReady = false
        stopObservingPlayback()
        completionObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        completionObserver = null
        player?.let {
            it.pause()
            it.replaceCurrentItemWithPlayerItem(null)
        }
        player = null
        currentAudio = null
        pendingStart = null
        _isPlayingFlow.value = false
        lastPositionMs = 0
    }

    override suspend fun setPlaybackSpeed(speed: Float) = withContext(Dispatchers.Main) {
        playbackSpeed = speed
        if (playWhenReady) player?.let { it.rate = speed }
        Unit
    }

    private fun playInternal(audio: AudioItem, positionMs: Long, speed: Float, revision: Long) {
        try {
            configureSession()
            playbackSpeed = speed
            val url = NSURL.fileURLWithPath(audio.path)
            val item = AVPlayerItem(uRL = url)
            pendingStart = audio to revision
            val existing = player
            val source = if (existing != null) {
                existing.replaceCurrentItemWithPlayerItem(item)
                existing.seekToTime(CMTimeMake(positionMs, 1000))
                existing
            } else {
                val newPlayer = AVPlayer(playerItem = item)
                if (positionMs > 0) newPlayer.seekToTime(CMTimeMake(positionMs, 1000))
                player = newPlayer
                newPlayer
            }
            currentAudio = audio
            bindRevision(revision)
            playWhenReady = true
            observePlayback()
            source.play()
            source.rate = speed
            _isPlayingFlow.value = true

        } catch (e: Exception) {
            AudioEngineHost.stopProgressReporting(currentPath, activeRevision, progress)
            stopObservingPlayback()
            pendingStart = null
            playWhenReady = false
            _isPlayingFlow.value = false
            throw e
        }
    }

    private fun configureSession() {
        check(AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, null)) { "Audio session configuration failed" }
    }

    private fun observePlayback() {
        stopObservingPlayback()
        val source = player ?: return
        val item = source.currentItem ?: return
        val observer = source.addPeriodicTimeObserverForInterval(CMTimeMake(1, 1), queue = null) {
            if (player == source && source.currentItem == item) {
                if (source.timeControlStatus == AVPlayerTimeControlStatusPlaying) flushPendingStart()
                if (pendingStart == null) {
                    AudioEngineHost.reportProgress(currentPath, activeRevision, mediaTimeMs(source.currentTime()))
                }
            }
        }
        playbackObserver = source to observer
    }

    private fun flushPendingStart() {
        val pending = pendingStart ?: return
        pendingStart = null
        AudioEngineHost.reportStarted(pending.first.path, pending.second)
    }

    private fun stopObservingPlayback() {
        playbackObserver?.let { (source, observer) -> source.removeTimeObserver(observer) }
        playbackObserver = null
    }
}
