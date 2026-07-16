package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.MutableState
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.events.ChannelInviteReceivedEvent
import com.ismartcoding.plain.events.ConfirmToAcceptLoginEvent

/**
 * Common interface for the main view-model surface used by commonMain pages.
 *
 * The concrete [MainViewModel] in commonMain implements this interface and is
 * used on both Android and iOS. On iOS the HTTP server platform abstractions
 * are no-ops, so enabling/syncing the server is a no-op without any extra
 * platform-specific subclass.
 */
interface MainViewModelBase {
    val httpServerError: MutableState<String>
    val httpServerState: MutableState<HttpServerState>
    val isVPNConnected: MutableState<Boolean>
    val ip4s: MutableState<List<String>>
    val ip4: MutableState<String>

    /** Currently-selected root tab index, used by MainNavGraph for back-stack handling. */
    val currentRootTab: MutableState<Int>

    /** Pending login confirmation event awaiting user accept/deny, or null. */
    val pendingLoginEvent: MutableState<ConfirmToAcceptLoginEvent?>

    /** Pending pairing request awaiting user accept/deny, or null. */
    val pendingPairingRequest: MutableState<DPairingRequest?>

    /**
     * The channel invite currently on top of the back stack (if any). Used by
     * ChannelInviteCanceledEvent handling to pop the right page. Not saved across
     * process death — a fresh invite will re-fire ChannelInviteReceivedEvent.
     */
    val pendingChannelInvite: MutableState<ChannelInviteReceivedEvent?>

    /** Enable/disable the embedded HTTP server. */
    fun enableHttpServer(enable: Boolean)

    /** Re-check and sync the HTTP server state (e.g. on page load). */
    fun syncHttpServerState()
}
