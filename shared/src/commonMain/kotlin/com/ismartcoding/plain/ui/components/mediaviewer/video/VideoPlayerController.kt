package com.ismartcoding.plain.ui.components.mediaviewer.video

/**
 * Platform-agnostic video player controller.
 * Android: backed by ExoPlayer; iOS: backed by AVPlayer.
 */
interface VideoPlayerController {
    fun play()
    fun pause()
    fun stop()
    fun prepare()
    fun seekTo(positionMs: Long)
    fun setPlaybackSpeed(speed: Float)
    fun setMuted(muted: Boolean)
    fun release()
    fun setMediaItem(path: String)
    fun setEventListener(listener: (VideoPlayerEvent) -> Unit)
    fun requestAudioFocus()
    fun abandonAudioFocus()

    val duration: Long
    val currentPosition: Long
    val bufferedPercentage: Int
    val isPlaying: Boolean
}
