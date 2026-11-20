package com.ismartcoding.plain.ui.nearby

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.features.chat.peer.PeerManager
import com.ismartcoding.plain.features.nearby.PairingMode
import com.ismartcoding.plain.features.nearby.RustPairingRuntime
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.models.NearbyDevice
import com.ismartcoding.plain.models.PairingProgressNotice
import com.ismartcoding.plain.platform.network.ensureBlePermissionAsync
import com.ismartcoding.plain.ui.state.launchSafe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow

object NearbyViewModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val nearbyDevices = MutableStateFlow<List<NearbyDevice>>(emptyList())
    var isDiscovering = mutableStateOf(false)
    val pairingViews = mutableStateMapOf<String, PairingView>()
    val itemStatus = mutableStateMapOf<String, NearbyItemStatus>()
    internal var nearbyAttempt by mutableStateOf<Pair<NearbyDevice, PairingMode>?>(null)
        private set


    private var pairingCommand: Job? = null
    private var blePermissionJob: Job? = null

    fun requestBlePermission() {
        if (blePermissionJob?.isActive == true) return
        blePermissionJob = scope.launchSafe(
            onError = {
                LogCat.e("BLE permission error: ${it.message}", it)
            },
            block = {
                ensureBlePermissionAsync()
            }
        )
    }

    internal fun startPairing(device: NearbyDevice, mode: PairingMode) {
        if (nearbyAttempt?.first?.id?.let { pairingViews[it]?.active } == true || pairingViews[device.id]?.active == true) return
        nearbyAttempt = device to mode
        PairingProjection.starting(device.id)
        submitPairing(device) { RustPairingRuntime.start(device, mode) }
    }

    internal fun confirmPairing(device: NearbyDevice) {
        if (pairingViews[device.id]?.step != PairingStep.TRUST_REQUIRED) return
        PairingProjection.trustAccepted(device.id)
        submitPairing(device) { RustPairingRuntime.respond(device.id, true) }
    }

    internal fun retryPairing(device: NearbyDevice, mode: PairingMode, password: String) {
        if (pairingViews[device.id]?.step != PairingStep.PASSWORD_REQUIRED) return
        PairingProjection.starting(device.id)
        submitPairing(device) { RustPairingRuntime.submit(device.id, mode, password) }
    }

    private fun submitPairing(device: NearbyDevice, send: suspend () -> Boolean) {
        val notice = PairingProgressNotice(device.id, device.name)
        val attempt = nearbyAttempt
        val previous = pairingCommand
        fun stillPending() = nearbyAttempt === attempt && pairingViews[device.id]?.active == true
        pairingCommand = scope.launchSafe(onError = { if (stillPending()) PairingProjection.failed(notice) }) {
            previous?.join()
            if (!stillPending()) return@launchSafe
            val started = send()
            if (!stillPending()) return@launchSafe
            if (!started) PairingProjection.failed(notice)
            else if (itemStatus[device.id] == NearbyItemStatus.STARTING) itemStatus[device.id] = NearbyItemStatus.PAIRING
        }
    }

    fun unpairPeer(deviceId: String) {
        val current = itemStatus[deviceId]
        if (current == NearbyItemStatus.UNPAIRING || current == NearbyItemStatus.PAIRING) return
        itemStatus[deviceId] = NearbyItemStatus.UNPAIRING
        scope.launchSafe(onDone = {
            itemStatus.remove(deviceId)
        }) {
            val removed = PeerManager.markUnpaired(deviceId)
            if (removed && com.ismartcoding.plain.ui.app.MainViewModel.selectedDevice.value?.peerId == deviceId) {
                com.ismartcoding.plain.ui.app.MainViewModel.selectDevice(null)
            }
        }
    }

    fun cancelPairing(deviceId: String) {
        val attempt = nearbyAttempt
        if (attempt?.first?.id != deviceId) {
            itemStatus.remove(deviceId)
            scope.launchSafe { RustPairingRuntime.cancel(deviceId) }
            return
        }
        val previous = pairingCommand
        pairingCommand = scope.launchSafe {
            previous?.join()
            if (nearbyAttempt !== attempt || pairingViews[deviceId]?.active != true) return@launchSafe
            if (RustPairingRuntime.cancel(deviceId) && nearbyAttempt === attempt && pairingViews[deviceId]?.active == true) {
                pairingViews.remove(deviceId)
                itemStatus.remove(deviceId)
            }
        }
    }

    fun handlePairingSuccess(deviceId: String) {
        itemStatus[deviceId] = NearbyItemStatus.PAIRED
    }
}
