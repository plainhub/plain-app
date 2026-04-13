package com.ismartcoding.plain.features.dlna

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow

enum class DlnaPlaybackState { NO_MEDIA_PRESENT, STOPPED, PLAYING, PAUSED, TRANSITIONING }

object DlnaRendererState {
    val isRunning = MutableStateFlow(false)
    val mediaUri = MutableStateFlow("")
    val mediaTitle = MutableStateFlow("")
    val mediaAlbumArtUri = MutableStateFlow("")
    val mediaType = MutableStateFlow(DlnaMediaType.UNKNOWN)
    val playbackState = MutableStateFlow(DlnaPlaybackState.NO_MEDIA_PRESENT)
    val port = MutableStateFlow(7878)
    val currentPositionMs = MutableStateFlow(0L)
    val durationMs = MutableStateFlow(0L)
    /** Non-null signals the player to seek to this position (milliseconds). */
    val seekTargetMs = MutableStateFlow<Long?>(null)

    /** Commands dispatched by the HTTP server; unlimited capacity to never drop. */
    val commandChannel = Channel<DlnaCommand>(Channel.UNLIMITED)

    /**
     * Set by the HTTP server when a cast request arrives. The ViewModel checks rules against
     * this and either silently handles it or promotes to [pendingCastRequest] for the UI.
     */
    val rawPendingCastRequest = MutableStateFlow<PendingCastRequest?>(null)

    /** Non-null when a cast request is shown to the user for confirmation. */
    val pendingCastRequest = MutableStateFlow<PendingCastRequest?>(null)

    /** True when a Play command arrived while a cast request was pending. */
    val pendingPlayQueued = MutableStateFlow(false)

    /** Non-empty when the receiver failed to start on all attempted ports. */
    val startError = MutableStateFlow("")

    fun reset() {
        mediaUri.value = ""
        mediaTitle.value = ""
        mediaAlbumArtUri.value = ""
        mediaType.value = DlnaMediaType.UNKNOWN
        playbackState.value = DlnaPlaybackState.NO_MEDIA_PRESENT
        currentPositionMs.value = 0L
        durationMs.value = 0L
        seekTargetMs.value = null
        rawPendingCastRequest.value = null
        pendingCastRequest.value = null
        pendingPlayQueued.value = false
        startError.value = ""
    }

    fun formatPositionInfo(): Pair<String, String> {
        val pos = currentPositionMs.value
        val dur = durationMs.value
        return Pair(formatTime(pos), if (dur > 0) formatTime(dur) else "00:00:00")
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
}
