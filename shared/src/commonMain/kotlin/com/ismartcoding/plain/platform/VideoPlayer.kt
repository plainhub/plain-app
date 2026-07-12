package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoPlayerController
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoState

/**
 * Creates and remembers a platform-specific [VideoPlayerController].
 * Android: ExoPlayer with cache + audio focus; iOS: AVPlayer.
 */
@Composable
expect fun rememberVideoPlayerController(): VideoPlayerController

/**
 * Platform-specific video rendering surface.
 * Android: AndroidView wrapping PlayerView; iOS: UIView wrapping AVPlayerLayer.
 *
 * @param useController whether to show the platform player's built-in controls
 */
@Composable
expect fun VideoPlayerSurface(
    modifier: Modifier,
    controller: VideoPlayerController,
    videoState: VideoState,
    useController: Boolean,
)
