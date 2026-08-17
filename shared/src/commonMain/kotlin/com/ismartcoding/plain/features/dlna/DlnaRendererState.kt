package com.ismartcoding.plain.features.dlna

import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.dlna.DlnaCommand
import com.ismartcoding.plain.lib.dlna.DlnaMediaType
import com.ismartcoding.plain.lib.dlna.PendingCastRequest
import com.ismartcoding.plain.preferences.DlnaAllowedSendersPreference
import com.ismartcoding.plain.preferences.DlnaDeniedSendersPreference
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow

enum class DlnaPlaybackState { NO_MEDIA_PRESENT, STOPPED, PLAYING, PAUSED, TRANSITIONING }

object DlnaRendererState {
    val isRunning = MutableStateFlow(false)
    val isRetrying = MutableStateFlow(false)
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
     * Set by the HTTP server when a cast request arrives. [DlnaReceiverEngine] checks
     * rules against this and either silently handles it or promotes to
     * [pendingCastRequest] for the UI.
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

    /**
     * Accepts the current [pendingCastRequest]: dispatches the SetUri (and queued
     * Play) commands and optionally persists the sender as allowed so future
     * requests from the same sender are auto-accepted.
     */
    fun acceptCastRequest(rememberChoice: Boolean) {
        val pending = pendingCastRequest.value ?: return
        val playQueued = pendingPlayQueued.value
        pendingCastRequest.value = null
        pendingPlayQueued.value = false
        commandChannel.trySend(DlnaCommand.SetUri(pending.mediaUri, pending.mediaTitle, pending.mediaType, pending.albumArtUri))
        if (playQueued) commandChannel.trySend(DlnaCommand.Play)
        if (rememberChoice && pending.senderIp.isNotEmpty()) {
            coIO {
                DlnaDeniedSendersPreference.removeAsync(pending.senderIp)
                DlnaAllowedSendersPreference.addAsync(pending.senderIp, pending.senderName)
            }
        }
    }

    /**
     * Rejects the current [pendingCastRequest] and optionally persists the sender
     * as denied so future requests from the same sender are silently discarded.
     */
    fun rejectCastRequest(rememberChoice: Boolean) {
        val pending = pendingCastRequest.value ?: return
        pendingCastRequest.value = null
        pendingPlayQueued.value = false
        if (rememberChoice && pending.senderIp.isNotEmpty()) {
            coIO {
                DlnaAllowedSendersPreference.removeAsync(pending.senderIp)
                DlnaDeniedSendersPreference.addAsync(pending.senderIp, pending.senderName)
            }
        }
    }

    fun formatPositionInfo(): Pair<String, String> {
        val pos = currentPositionMs.value
        val dur = durationMs.value
        return Pair(TimeHelper.formatTime(pos), if (dur > 0) TimeHelper.formatTime(dur) else "00:00:00")
    }
}
