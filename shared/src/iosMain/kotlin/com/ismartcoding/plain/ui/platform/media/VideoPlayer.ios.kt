@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.ui.platform.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.ismartcoding.plain.ui.media.viewer.video.VideoPlayerController
import com.ismartcoding.plain.ui.media.viewer.video.VideoPlayerEvent
import com.ismartcoding.plain.ui.media.viewer.video.VideoState
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.muted
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.playbackLikelyToKeepUp
import platform.AVFoundation.rate
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTimeMake
import com.ismartcoding.plain.platform.media.mediaTimeMs
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSCoder
import com.ismartcoding.plain.lib.coMain
import com.ismartcoding.plain.platform.system.IosPlatformRegistry
import platform.Foundation.NSURL
import platform.UIKit.UIView

private class AVPlayerVideoController : VideoPlayerController {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var player by mutableStateOf<AVPlayer?>(null)
    private var firstFrameReported = false
    private var pollJob: Job? = null
    private var eventListener: ((VideoPlayerEvent) -> Unit)? = null
    private var lastIsPlaying = false

    override fun play() {
        val p = player ?: return
        p.play()
        startPolling()
    }

    override fun pause() {
        val p = player ?: return
        p.pause()
        stopPolling()
    }

    override fun stop() {
        val p = player ?: return
        p.pause()
        p.seekToTime(CMTimeMake(0, 1000))
        stopPolling()
    }

    override fun prepare() {}

    override fun seekTo(positionMs: Long) {
        val p = player ?: return
        p.seekToTime(CMTimeMake(positionMs, 1000))
        eventListener?.invoke(VideoPlayerEvent.PositionDiscontinuity)
    }

    override fun setPlaybackSpeed(speed: Float) {
        val p = player ?: return
        p.rate = speed
    }

    override fun setMuted(muted: Boolean) {
        val p = player ?: return
        p.muted = muted
    }

    override fun release() {
        stopPolling()
        val p = player
        if (p != null) {
            p.pause()
            p.replaceCurrentItemWithPlayerItem(null)
        }
        player = null
        eventListener = null
    }

    override fun setMediaItem(path: String, tlsCertificateSha256: String?) {
        firstFrameReported = false
        val url = if (path.startsWith("http")) {
            NSURL.URLWithString(path)
        } else {
            NSURL.fileURLWithPath(path)
        } ?: return
        val item = AVPlayerItem(uRL = url)
        val existing = player
        if (existing != null) {
            existing.replaceCurrentItemWithPlayerItem(item)
        } else {
            val newPlayer = AVPlayer(playerItem = item)
            player = newPlayer
        }
    }

    override fun setEventListener(listener: (VideoPlayerEvent) -> Unit) {
        eventListener = listener
    }

    override fun requestAudioFocus() {}
    override fun abandonAudioFocus() {}

    override val durationMs: Long
        get() = player?.currentItem?.let { mediaTimeMs(it.duration) } ?: 0L

    override val currentPositionMs: Long
        get() = player?.let { mediaTimeMs(it.currentTime()) } ?: 0L

    override val bufferedPercentage: Int get() = 0
    override val isPlaying: Boolean
        get() = player?.let { it.rate > 0f } ?: false
    override val isBuffering: Boolean
        get() {
            val p = player ?: return false
            val item = p.currentItem ?: return false
            return !item.playbackLikelyToKeepUp
        }

    private fun startPolling() {
        stopPolling()
        pollJob = scope.launch {
            while (isActive) {
                delay(500)
                val p = player ?: break
                val playing = p.rate > 0f
                if (playing != lastIsPlaying) {
                    lastIsPlaying = playing
                    eventListener?.invoke(
                        VideoPlayerEvent.StateChanged(
                            isPlaying = playing,
                            durationMs = durationMs,
                        ),
                    )
                }
                if (!playing) break
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun getPlayer(): AVPlayer? = player

    fun onFirstFrameRendered(item: AVPlayerItem?) {
        if (item == null || item != player?.currentItem || firstFrameReported) return
        firstFrameReported = true
        eventListener?.invoke(VideoPlayerEvent.FirstFrameRendered)
    }
}

@Composable
actual fun rememberVideoPlayerController(claimAudioSession: Boolean): VideoPlayerController {
    return remember { AVPlayerVideoController() }
}

@Composable
actual fun VideoPlayerSurface(
    modifier: Modifier,
    controller: VideoPlayerController,
    videoState: VideoState?,
) {
    val avController = controller as? AVPlayerVideoController ?: return
    val player = avController.getPlayer()
    UIKitView(
        modifier = modifier,
        factory = {
            PlayerContainerView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
                attachPlayer(player, avController::onFirstFrameRendered)
            }
        },
        update = { view ->
            view.attachPlayer(player, avController::onFirstFrameRendered)
        },
        onRelease = { it.release() },
    )
}

@OptIn(BetaInteropApi::class)
internal class PlayerContainerView : UIView {
    @OverrideInit
    constructor(frame: CValue<CGRect>) : super(frame)

    @OverrideInit
    constructor(coder: NSCoder) : super(coder)

    private val playerLayer = AVPlayerLayer()
    private var onFirstFrame: ((AVPlayerItem?) -> Unit)? = null
    private val readinessObservation = IosPlatformRegistry.observeVideoFrame(playerLayer) {
        val item = playerLayer.player?.currentItem
        coMain { onFirstFrame?.invoke(item) }
    }

    init {
        playerLayer.videoGravity = AVLayerVideoGravityResizeAspect
        layer.addSublayer(playerLayer)
    }

    fun attachPlayer(player: AVPlayer?, onFirstFrame: (AVPlayerItem?) -> Unit) {
        this.onFirstFrame = onFirstFrame
        if (playerLayer.player != player) {
            playerLayer.player = player
            if (playerLayer.readyForDisplay) onFirstFrame(player?.currentItem)
        }
    }

    fun release() {
        onFirstFrame = null
        readinessObservation.close()
        playerLayer.player = null
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        playerLayer.setFrame(this.bounds)
    }
}
