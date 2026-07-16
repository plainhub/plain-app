package com.ismartcoding.plain.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.features.dlna.DlnaCommand
import com.ismartcoding.plain.features.dlna.DlnaPlaybackState
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.platform.getPlayerDurationMs
import com.ismartcoding.plain.platform.getPlayerPositionMs
import com.ismartcoding.plain.platform.startDlnaRenderer
import com.ismartcoding.plain.platform.stopDlnaRenderer
import com.ismartcoding.plain.preferences.DlnaAllowedSendersPreference
import com.ismartcoding.plain.preferences.DlnaDeniedSendersPreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class DlnaReceiverViewModel : ViewModel() {

    val isRetrying = MutableStateFlow(false)

    private var commandJob: Job? = null
    private var positionJob: Job? = null
    private var ruleCheckJob: Job? = null

    init {
        startCommandProcessing()
    }

    fun startReceiver() {
        startDlnaRenderer()
        startCommandProcessing()
        startRuleCheck()
    }

    fun stopReceiver() {
        commandJob?.cancel()
        positionJob?.cancel()
        ruleCheckJob?.cancel()
        stopDlnaRenderer()
    }

    fun retryReceiver() {
        viewModelScope.launch {
            isRetrying.value = true
            stopDlnaRenderer()
            startDlnaRenderer()
            startCommandProcessing()
            startRuleCheck()
            delay(300)
            isRetrying.value = false
        }
    }

    private fun startRuleCheck() {
        ruleCheckJob?.cancel()
        ruleCheckJob = launchSafe {
            DlnaRendererState.rawPendingCastRequest.filterNotNull().collect { pending ->
                val allowed = DlnaAllowedSendersPreference.getAsync()
                val denied = DlnaDeniedSendersPreference.getAsync()
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

    fun acceptCastRequest(rememberChoice: Boolean) {
        val pending = DlnaRendererState.pendingCastRequest.value ?: return
        val playQueued = DlnaRendererState.pendingPlayQueued.value
        DlnaRendererState.pendingCastRequest.value = null
        DlnaRendererState.pendingPlayQueued.value = false
        DlnaRendererState.commandChannel.trySend(DlnaCommand.SetUri(pending.mediaUri, pending.mediaTitle, pending.mediaType, pending.albumArtUri))
        if (playQueued) {
            DlnaRendererState.commandChannel.trySend(DlnaCommand.Play)
        }
        if (rememberChoice && pending.senderIp.isNotEmpty()) {
            launchSafe {
                DlnaDeniedSendersPreference.removeAsync(pending.senderIp)
                DlnaAllowedSendersPreference.addAsync(pending.senderIp, pending.senderName)
            }
        }
    }

    fun rejectCastRequest(rememberChoice: Boolean) {
        val pending = DlnaRendererState.pendingCastRequest.value ?: return
        DlnaRendererState.pendingCastRequest.value = null
        DlnaRendererState.pendingPlayQueued.value = false
        if (rememberChoice && pending.senderIp.isNotEmpty()) {
            launchSafe {
                DlnaAllowedSendersPreference.removeAsync(pending.senderIp)
                DlnaDeniedSendersPreference.addAsync(pending.senderIp, pending.senderName)
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

    fun startPositionSync(player: Any?) {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (true) {
                DlnaRendererState.currentPositionMs.value = getPlayerPositionMs(player)
                DlnaRendererState.durationMs.value = getPlayerDurationMs(player)
                delay(1_000)
            }
        }
    }
}
