package com.ismartcoding.plain.ui.app

import com.ismartcoding.plain.models.PairingRequestNotice
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.features.sessions.WebLoginRequestFacts
import com.ismartcoding.plain.models.WebAccessRequest
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.search.GlobalSearchHit

internal data class DeviceIdentity(val peerId: String, val publicKey: String)
object MainViewModel {
    internal val selectedDevice = mutableStateOf<DeviceIdentity?>(null)
    internal val pendingSearchHit = mutableStateOf<GlobalSearchHit?>(null)

    internal fun selectDevice(device: DeviceIdentity?) {
        if (selectedDevice.value == device) return
        pendingSearchHit.value = null
        currentRootTab.intValue = 1
        selectedDevice.value = device
    }

    internal fun openSearchHit(hit: GlobalSearchHit) {
        val client = (hit.device as? ContentSource.Remote)?.client
        selectDevice(client?.let { DeviceIdentity(it.peerId, it.publicKey) })
        pendingSearchHit.value = hit
    }

    lateinit var relaunchApp: () -> Unit
        internal set
    lateinit var installAppUpdate: (String) -> Unit
        internal set
    lateinit var addMediaShortcut: (String, String) -> Unit
        internal set

    val shortcutMediaPath = mutableStateOf<String?>(null)
    val currentRootTab = mutableIntStateOf(1)
    val pendingLoginRequest = mutableStateOf<WebLoginRequestFacts?>(null)
    val pendingPairingRequest = mutableStateOf<PairingRequestNotice?>(null)
    val pendingChannelInvite = mutableStateOf<Routing.ChannelInviteRequest?>(null)
    val pendingWebAccessRequest = mutableStateOf<WebAccessRequest?>(null)
}
