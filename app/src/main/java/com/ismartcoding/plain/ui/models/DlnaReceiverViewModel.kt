package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.ismartcoding.plain.features.dlna.DlnaCommand
import com.ismartcoding.plain.features.dlna.DlnaPlaybackState
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.features.dlna.receiver.DlnaRenderer
import com.ismartcoding.plain.preferences.DlnaAllowedSendersPreference
import com.ismartcoding.plain.preferences.DlnaDeniedSendersPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class DlnaReceiverViewModel : ViewModel() {

    private var commandJob: Job? = null
    private var positionJob: Job? = null
    private var ruleCheckJob: Job? = null

    init {
        startCommandProcessing()
    }

    fun startReceiver(context: Context) {
        DlnaRenderer.start(context)
        startCommandProcessing()
        startRuleCheck(context)
    }

    fun stopReceiver(context: Context) {
        commandJob?.cancel()
        positionJob?.cancel()
        ruleCheckJob?.cancel()
        DlnaRenderer.stop()
    }

    fun retryReceiver(context: Context) {
        DlnaRenderer.stop()
        DlnaRenderer.start(context)
        startCommandProcessing()
        startRuleCheck(context)
    }

    private fun startRuleCheck(context: Context) {
        ruleCheckJob?.cancel()
        ruleCheckJob = viewModelScope.launch(Dispatchers.IO) {
            DlnaRendererState.rawPendingCastRequest.filterNotNull().collect { pending ->
                val allowed = DlnaAllowedSendersPreference.getAsync(context)
                val denied = DlnaDeniedSendersPreference.getAsync(context)
                when {
                    DlnaAllowedSendersPreference.containsIp(allowed, pending.senderIp) -> {
                        // Auto-accept: send commands directly without showing dialog
                        DlnaRendererState.pendingCastRequest.value = null
                        DlnaRendererState.rawPendingCastRequest.value = null
                        val playQueued = DlnaRendererState.pendingPlayQueued.value
                        DlnaRendererState.pendingPlayQueued.value = false
                        DlnaRendererState.commandChannel.trySend(DlnaCommand.SetUri(pending.mediaUri, pending.mediaTitle, pending.mediaType, pending.albumArtUri))
                        if (playQueued) DlnaRendererState.commandChannel.trySend(DlnaCommand.Play)
                    }
                    DlnaDeniedSendersPreference.containsIp(denied, pending.senderIp) -> {
                        // Auto-reject: silently discard, no dialog shown
                        DlnaRendererState.rawPendingCastRequest.value = null
                        DlnaRendererState.pendingPlayQueued.value = false
                    }
                    else -> {
                        // Unknown sender: promote to UI-visible state for user decision
                        DlnaRendererState.pendingCastRequest.value = pending
                        DlnaRendererState.rawPendingCastRequest.value = null
                    }
                }
            }
        }
    }

    fun acceptCastRequest(context: Context, rememberChoice: Boolean) {
        val pending = DlnaRendererState.pendingCastRequest.value ?: return
        val playQueued = DlnaRendererState.pendingPlayQueued.value
        DlnaRendererState.pendingCastRequest.value = null
        DlnaRendererState.pendingPlayQueued.value = false
        DlnaRendererState.commandChannel.trySend(DlnaCommand.SetUri(pending.mediaUri, pending.mediaTitle, pending.mediaType, pending.albumArtUri))
        if (playQueued) {
            DlnaRendererState.commandChannel.trySend(DlnaCommand.Play)
        }
        if (rememberChoice && pending.senderIp.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                DlnaDeniedSendersPreference.removeAsync(context, pending.senderIp)
                DlnaAllowedSendersPreference.addAsync(context, pending.senderIp, pending.senderName)
            }
        }
    }

    fun rejectCastRequest(context: Context, rememberChoice: Boolean) {
        val pending = DlnaRendererState.pendingCastRequest.value ?: return
        DlnaRendererState.pendingCastRequest.value = null
        DlnaRendererState.pendingPlayQueued.value = false
        if (rememberChoice && pending.senderIp.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                DlnaAllowedSendersPreference.removeAsync(context, pending.senderIp)
                DlnaDeniedSendersPreference.addAsync(context, pending.senderIp, pending.senderName)
            }
        }
    }

    fun startCommandProcessing() {
        commandJob?.cancel()
        commandJob = viewModelScope.launch {
            for (command in DlnaRendererState.commandChannel) {
                when (command) {
                    is DlnaCommand.SetUri -> {
                        DlnaRendererState.mediaUri.value = command.uri
                        DlnaRendererState.mediaTitle.value = command.title
                        DlnaRendererState.mediaAlbumArtUri.value = command.albumArtUri
                        DlnaRendererState.mediaType.value = command.mediaType
                        DlnaRendererState.playbackState.value = DlnaPlaybackState.TRANSITIONING
                    }
                    is DlnaCommand.Play -> DlnaRendererState.playbackState.value = DlnaPlaybackState.PLAYING
                    is DlnaCommand.Pause -> DlnaRendererState.playbackState.value = DlnaPlaybackState.PAUSED
                    is DlnaCommand.Stop -> {
                        DlnaRendererState.seekTargetMs.value = 0L
                        DlnaRendererState.playbackState.value = DlnaPlaybackState.STOPPED
                    }
                    is DlnaCommand.Seek -> DlnaRendererState.seekTargetMs.value = command.positionMs
                }
            }
        }
    }

    fun startPositionSync(player: ExoPlayer) {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (true) {
                DlnaRendererState.currentPositionMs.value = player.currentPosition.coerceAtLeast(0L)
                DlnaRendererState.durationMs.value = player.duration.coerceAtLeast(0L)
                delay(1_000)
            }
        }
    }
}



