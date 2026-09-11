package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.events.ConfirmToAcceptLoginEvent
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.events.ChannelInviteReceivedEvent

/**
 * UI-only state for the main screen. HTTP server lifecycle (state, error,
 * start/stop/restore intent) lives entirely in
 * [com.ismartcoding.plain.httpserver.HttpServerManager]; pages read its flows
 * directly and dispatch commands through it.
 */
class MainViewModel : ViewModel() {
    var isVPNConnected = mutableStateOf(false)
    var currentRootTab = mutableIntStateOf(0)
    var pendingLoginEvent = mutableStateOf<ConfirmToAcceptLoginEvent?>(null)
    var pendingPairingRequest = mutableStateOf<DPairingRequest?>(null)
    // The channel invite currently on top of the back stack (if any). Used by
    // ChannelInviteCanceledEvent handling to pop the right page. Not saved across
    // process death — a fresh invite will re-fire ChannelInviteReceivedEvent.
    var pendingChannelInvite = mutableStateOf<ChannelInviteReceivedEvent?>(null)
}
