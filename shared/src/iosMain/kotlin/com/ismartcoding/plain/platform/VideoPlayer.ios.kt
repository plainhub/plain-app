@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoPlayerController
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoPlayerEvent
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoState
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
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
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSCoder
import platform.Foundation.NSURL
import platform.UIKit.UIView
import platform.darwin.NSObject

private class AVPlayerVideoController : VideoPlayerController {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var player: AVPlayer? = null
    private var pollJob: Job? = null
    private var eventListener: ((VideoPlayerEvent) -> Unit)? = null
    private var lastIsPlaying = false

    private fun ensurePlayer(): AVPlayer? = player

    override fun play() {
        val p = player ?: return
        avPlayerPerform(p, "play")
        startPolling()
    }

    override fun pause() {
        val p = player ?: return
        avPlayerPerform(p, "pause")
        stopPolling()
    }

    override fun stop() {
        val p = player ?: return
        avPlayerPerform(p, "pause")
        avPlayerSeekToMs(p, 0L)
        stopPolling()
    }

    override fun prepare() {}

    override fun seekTo(positionMs: Long) {
        val p = player ?: return
        avPlayerSeekToMs(p, positionMs)
        eventListener?.invoke(VideoPlayerEvent.PositionDiscontinuity)
    }

    override fun setPlaybackSpeed(speed: Float) {
        val p = player ?: return
        avPlayerSetRate(p, speed)
    }

    override fun setMuted(muted: Boolean) {
        val p = player ?: return
        avPlayerSetMuted(p, muted)
    }

    override fun release() {
        stopPolling()
        val p = player
        if (p != null) {
            avPlayerPerform(p, "pause")
            avPlayerPerformWithArg(p, "replaceCurrentItemWithPlayerItem:", null)
        }
        player = null
        eventListener = null
    }

    override fun setMediaItem(path: String) {
        val url = if (path.startsWith("http")) {
            NSURL.URLWithString(path)
        } else {
            NSURL.fileURLWithPath(path)
        } ?: return
        val item = AVPlayerItem(uRL = url)
        val existing = player
        if (existing != null) {
            avPlayerPerformWithArg(existing, "replaceCurrentItemWithPlayerItem:", item)
        } else {
            val newPlayer = AVPlayer(uRL = url)
            player = newPlayer
        }
    }

    override fun setEventListener(listener: (VideoPlayerEvent) -> Unit) {
        eventListener = listener
    }

    override fun requestAudioFocus() {}
    override fun abandonAudioFocus() {}

    override val duration: Long
        get() = player?.let { avPlayerTimeMs(it, "duration") } ?: 0L

    override val currentPosition: Long
        get() = player?.let { avPlayerTimeMs(it, "currentTime") } ?: 0L

    override val bufferedPercentage: Int get() = 0
    override val isPlaying: Boolean
        get() = player?.let { avPlayerRate(it) > 0f } ?: false

    private fun startPolling() {
        stopPolling()
        pollJob = scope.launch {
            while (isActive) {
                delay(500)
                val p = player ?: break
                val playing = avPlayerRate(p) > 0f
                if (playing != lastIsPlaying) {
                    lastIsPlaying = playing
                    eventListener?.invoke(
                        VideoPlayerEvent.StateChanged(
                            isPlaying = playing,
                            duration = duration,
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

    fun getPlayer(): NSObject? = player
}

@Composable
actual fun rememberVideoPlayerController(): VideoPlayerController {
    return remember { AVPlayerVideoController() }
}

@Composable
actual fun VideoPlayerSurface(
    modifier: Modifier,
    controller: VideoPlayerController,
    videoState: VideoState,
    useController: Boolean,
) {
    val avController = controller as? AVPlayerVideoController ?: return
    UIKitView(
        modifier = modifier,
        factory = {
            PlayerContainerView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
                attachPlayer(avController.getPlayer())
            }
        },
        update = { view ->
            view.attachPlayer(avController.getPlayer())
        },
    )
}

internal class PlayerContainerView : UIView {
    @OverrideInit
    constructor(frame: CValue<CGRect>) : super(frame)

    @OverrideInit
    constructor(coder: NSCoder) : super(coder)

    private val playerLayer = AVPlayerLayer()

    init {
        playerLayer.videoGravity = AVLayerVideoGravityResizeAspect
        layer.addSublayer(playerLayer)
    }

    fun attachPlayer(player: NSObject?) {
        playerLayer.player = player as? AVPlayer
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        playerLayer.setFrame(this.bounds)
    }
}
