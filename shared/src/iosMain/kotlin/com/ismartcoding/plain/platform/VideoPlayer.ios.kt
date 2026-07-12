package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoPlayerController
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoPlayerEvent
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoState

/**
 * iOS stub implementation of [VideoPlayerController].
 * Will be replaced with AVPlayer-backed implementation when iOS video playback is implemented.
 */
private class AVPlayerVideoController : VideoPlayerController {
    override fun play() {}
    override fun pause() {}
    override fun stop() {}
    override fun prepare() {}
    override fun seekTo(positionMs: Long) {}
    override fun setPlaybackSpeed(speed: Float) {}
    override fun setMuted(muted: Boolean) {}
    override fun release() {}
    override fun setMediaItem(path: String) {}
    override fun setEventListener(listener: (VideoPlayerEvent) -> Unit) {}
    override fun requestAudioFocus() {}
    override fun abandonAudioFocus() {}

    override val duration: Long get() = 0L
    override val currentPosition: Long get() = 0L
    override val bufferedPercentage: Int get() = 0
    override val isPlaying: Boolean get() = false
}

@Composable
actual fun rememberVideoPlayerController(): VideoPlayerController {
    return androidx.compose.runtime.remember { AVPlayerVideoController() }
}

@Composable
actual fun VideoPlayerSurface(
    modifier: Modifier,
    controller: VideoPlayerController,
    videoState: VideoState,
    useController: Boolean,
) {
    // TODO: Implement AVPlayerLayer-backed surface when iOS video playback is implemented
}
