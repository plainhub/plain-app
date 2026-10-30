package com.ismartcoding.plain.ui.app

import com.ismartcoding.plain.models.PairingRequestInput
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.features.sessions.WebLoginRequestFacts
import com.ismartcoding.plain.ui.navigation.Routing

object MainViewModel {
    lateinit var relaunchApp: () -> Unit
        internal set
    lateinit var installAppUpdate: (String) -> Unit
        internal set
    lateinit var addMediaShortcut: (String, String) -> Unit
        internal set

    val shortcutMediaPath = mutableStateOf<String?>(null)
    val currentRootTab = mutableIntStateOf(0)
    val pendingLoginRequest = mutableStateOf<WebLoginRequestFacts?>(null)
    val pendingPairingRequest = mutableStateOf<PairingRequestInput?>(null)
    val pendingChannelInvite = mutableStateOf<Routing.ChannelInviteRequest?>(null)
    val pendingWebAccessRequest = mutableStateOf<com.ismartcoding.plain.models.WebAccessRequest?>(null)
}
