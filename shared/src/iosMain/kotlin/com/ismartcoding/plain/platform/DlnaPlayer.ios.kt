@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL

/**
 * iOS [DlnaMediaEngine] backed by a raw AVPlayer (no session/focus
 * management), mirroring the Android [DlnaExoEngine]. Uses the shared
 * [avPlayerPerform]/[avPlayerSeekToMs]/[avPlayerTimeMs]/[avPlayerRate]
 * helpers so the playback plumbing stays identical to [VideoPlayerController]'s
 * iOS actual.
 */
private class DlnaAvEngine : DlnaMediaEngine {
    private var player: AVPlayer? = null

    override fun setMediaUri(uri: String) {
        val url = if (uri.startsWith("http")) {
            NSURL.URLWithString(uri)
        } else {
            NSURL.fileURLWithPath(uri)
        } ?: return
        val item = AVPlayerItem(uRL = url)
        val existing = player
        if (existing != null) {
            avPlayerPerformWithArg(existing, "replaceCurrentItemWithPlayerItem:", item)
        } else {
            player = AVPlayer(uRL = url)
        }
    }

    override fun play() { player?.let { avPlayerPerform(it, "play") } }
    override fun pause() { player?.let { avPlayerPerform(it, "pause") } }
    override fun stopAndRewind() {
        val p = player ?: return
        avPlayerPerform(p, "pause")
        avPlayerSeekToMs(p, 0L)
    }
    override fun seekTo(positionMs: Long) { player?.let { avPlayerSeekToMs(it, positionMs) } }
    override val positionMs: Long get() = player?.let { avPlayerTimeMs(it, "currentTime") } ?: 0L
    override val durationMs: Long get() = player?.let { avPlayerTimeMs(it, "duration") } ?: 0L
    override val isPlaying: Boolean get() = player?.let { avPlayerRate(it) > 0f } ?: false
    override fun release() {
        val p = player ?: return
        avPlayerPerform(p, "pause")
        avPlayerPerformWithArg(p, "replaceCurrentItemWithPlayerItem:", null)
        player = null
    }

    fun getPlayer(): AVPlayer? = player
}

@Composable
actual fun rememberDlnaMediaEngine(): DlnaMediaEngine = remember { DlnaAvEngine() }

@Composable
actual fun DlnaVideoSurface(engine: DlnaMediaEngine, modifier: Modifier) {
    val av = engine as? DlnaAvEngine ?: return
    UIKitView(
        modifier = modifier,
        factory = {
            PlayerContainerView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
                attachPlayer(av.getPlayer())
            }
        },
        update = { view ->
            view.attachPlayer(av.getPlayer())
        },
    )
}
