package com.ismartcoding.plain.ui.app

import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.preferences.PrefKeys
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.models.PairingRequestNotice
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.features.sessions.WebLoginRequestFacts
import com.ismartcoding.plain.models.WebAccessRequest
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.ui.search.GlobalSearchHit
import com.ismartcoding.plain.ui.state.launchSafe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class DeviceIdentity(val peerId: String, val publicKey: String)
object MainViewModel {
    internal val selectedDevice = mutableStateOf<DeviceIdentity?>(null)
    internal val pendingSearchHit = mutableStateOf<GlobalSearchHit?>(null)
    internal val restoringDeviceSelection = mutableStateOf(false)
    private val deviceSelectionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val deviceSwitch = Mutex()

    internal fun restoreDeviceSelectionOnStartup() {
        val peerId = Prefs.selectedDeviceId.value
        if (peerId.isEmpty()) return
        restoringDeviceSelection.value = true
        deviceSelectionScope.launchSafe(
            onError = { LogCat.e(it) },
            onDone = { restoringDeviceSelection.value = false },
        ) { restoreDeviceSelection(peerId) }
    }

    internal fun requestDeviceSwitch(device: DeviceIdentity?) {
        deviceSelectionScope.launchSafe { switchDevice(device) }
    }

    internal suspend fun restoreDeviceSelection(
        peerId: String,
        findPeer: suspend (String) -> Peer? = { id ->
            GraphqlApi.execute(Query.peerSummaries()).find { it.id == id }
        },
        save: suspend (String) -> Unit = ::saveDeviceSelection,
    ) {
        val peer = findPeer(peerId)
        if (peer == null || !peer.managed || peer.publicKey.isEmpty()) {
            save("")
        } else if (pendingLauncherShortcut.value == null) {
            selectDevice(DeviceIdentity(peer.id, peer.publicKey))
        }
    }

    internal suspend fun switchDevice(
        device: DeviceIdentity?,
        save: suspend (String) -> Unit = ::saveDeviceSelection,
    ) = deviceSwitch.withLock {
        save(device?.peerId.orEmpty())
        selectDevice(device)
    }

    internal suspend fun forgetUnpairedDevice(peerId: String) = deviceSwitch.withLock {
        if (selectedDevice.value?.peerId == peerId) selectDevice(null)
        if (Prefs.selectedDeviceId.value == peerId) saveDeviceSelection("")
    }

    private suspend fun saveDeviceSelection(peerId: String) {
        Prefs.set(PrefKeys.SELECTED_DEVICE_ID, peerId)
    }

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
    var addMediaShortcut: ((String, String) -> Unit)? = null
        internal set

    internal val pendingLauncherShortcut = mutableStateOf<AppFeatureType?>(null)

    internal fun openLauncherShortcut(type: AppFeatureType) {
        selectDevice(null)
        pendingSearchHit.value = null
        currentRootTab.intValue = 2
        pendingLauncherShortcut.value = type
    }

    val shortcutMediaPath = mutableStateOf<String?>(null)
    val currentRootTab = mutableIntStateOf(1)
    val pendingLoginRequest = mutableStateOf<WebLoginRequestFacts?>(null)
    val pendingPairingRequest = mutableStateOf<PairingRequestNotice?>(null)
    val pendingChannelInvite = mutableStateOf<Routing.ChannelInviteRequest?>(null)
    val pendingWebAccessRequest = mutableStateOf<WebAccessRequest?>(null)
}
