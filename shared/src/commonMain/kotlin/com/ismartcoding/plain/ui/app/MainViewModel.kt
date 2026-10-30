package com.ismartcoding.plain.ui.app

import com.ismartcoding.plain.models.PairingRequestNotice
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.features.sessions.WebLoginRequestFacts
import com.ismartcoding.plain.ui.navigation.Routing

internal data class DeviceIdentity(val peerId: String, val publicKey: String)
internal data class DeviceContentEntry(
    val feature: com.ismartcoding.plain.platform.system.AppFeatureType,
    val id: String = "",
    val path: String = "",
    val title: String = "",
)

object MainViewModel {
    internal val selectedDevice = mutableStateOf<DeviceIdentity?>(null)
    internal val pendingDeviceContent = mutableStateOf<DeviceContentEntry?>(null)

    internal fun selectDevice(device: DeviceIdentity?) {
        if (selectedDevice.value == device) return
        pendingDeviceContent.value = null
        currentRootTab.intValue = 1
        selectedDevice.value = device
    }

    internal fun openDeviceContent(device: DeviceIdentity?, entry: DeviceContentEntry) {
        selectDevice(device)
        pendingDeviceContent.value = entry
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
    val pendingWebAccessRequest = mutableStateOf<com.ismartcoding.plain.models.WebAccessRequest?>(null)
}
