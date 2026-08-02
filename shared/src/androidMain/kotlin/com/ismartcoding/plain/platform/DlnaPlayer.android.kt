package com.ismartcoding.plain.platform

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * Android [DlnaMediaEngine] backed by a raw ExoPlayer (no MediaSession, no
 * audio focus) — preserves the original DLNA receiver behavior of not
 * contending with the app's own audio player. The ExoPlayer instance is built
 * by the shared [rememberExoPlayer] factory (same config/cache as the main
 * video player).
 */
@OptIn(UnstableApi::class)
private class DlnaExoEngine(val exoPlayer: ExoPlayer) : DlnaMediaEngine {
    override fun setMediaUri(uri: String) {
        exoPlayer.playWhenReady = false
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
    }
    override fun play() = exoPlayer.play()
    override fun pause() = exoPlayer.pause()
    override fun stopAndRewind() { exoPlayer.pause(); exoPlayer.seekTo(0) }
    override fun seekTo(positionMs: Long) = exoPlayer.seekTo(positionMs)
    override val positionMs: Long get() = exoPlayer.currentPosition.coerceAtLeast(0L)
    override val durationMs: Long get() = exoPlayer.duration.coerceAtLeast(0L)
    override val isPlaying: Boolean get() = exoPlayer.isPlaying
    override fun release() { exoPlayer.stop(); exoPlayer.release() }
}

@OptIn(UnstableApi::class)
@Composable
actual fun rememberDlnaMediaEngine(): DlnaMediaEngine {
    val context = LocalContext.current
    val exoPlayer = rememberExoPlayer(context)
    return remember { DlnaExoEngine(exoPlayer) }
}

@OptIn(UnstableApi::class)
@Composable
actual fun DlnaVideoSurface(engine: DlnaMediaEngine, modifier: Modifier) {
    val exoPlayer = (engine as DlnaExoEngine).exoPlayer
    val context = LocalContext.current
    val playerView = remember { PlayerView(context) }
    AndroidView(
        modifier = modifier,
        factory = {
            playerView.apply {
                this.player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
    )
}
