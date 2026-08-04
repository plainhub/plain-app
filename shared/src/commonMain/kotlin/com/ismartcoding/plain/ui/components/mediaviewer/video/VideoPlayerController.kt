package com.ismartcoding.plain.ui.components.mediaviewer.video

/**
 * Platform-agnostic video player controller.
 * Android: backed by ExoPlayer; iOS: backed by AVPlayer.
 *
 * When created with `claimAudioSession = false` (via [rememberVideoPlayerController]),
 * the platform actual skips MediaSession / audio-focus management — used by the
 * DLNA receiver which must not contend with the app's own audio player.
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

    /** Pause and rewind to the start (matches DLNA STOPPED semantics). */
    fun stopAndRewind() {
        pause()
        seekTo(0L)
    }

    val duration: Long
    val currentPosition: Long
    val bufferedPercentage: Int
    val isPlaying: Boolean
    val isBuffering: Boolean
}
