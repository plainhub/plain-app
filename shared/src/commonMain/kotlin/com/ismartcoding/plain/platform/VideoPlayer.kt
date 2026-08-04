package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoPlayerController
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoState

/**
 * Creates and remembers a platform-specific [VideoPlayerController].
 * Android: ExoPlayer with cache + audio focus; iOS: AVPlayer.
 *
 * @param claimAudioSession when true (default), the platform actual claims a
 *   MediaSession and audio focus (for the local media previewer). When false,
 *   the controller is a "raw" engine with no session/focus management — used
 *   by the DLNA receiver which must not contend with the app's own audio player.
 */
@Composable
expect fun rememberVideoPlayerController(claimAudioSession: Boolean = true): VideoPlayerController

/**
 * Platform video rendering surface attached to [controller].
 *
 * @param videoState when non-null, the surface manages fullscreen orientation,
 *   PiP lifecycle, and keepScreenOn based on this state (for the local media
 *   previewer). When null, the surface renders only the player + buffering
 *   indicator — the caller manages lifecycle and fullscreen itself (DLNA receiver).
 */
@Composable
expect fun VideoPlayerSurface(
    modifier: Modifier,
    controller: VideoPlayerController,
    videoState: VideoState? = null,
)
