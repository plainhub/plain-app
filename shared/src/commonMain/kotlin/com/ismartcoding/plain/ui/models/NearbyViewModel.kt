package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.ble.PairingTransport
import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.discover.MdnsDiscoverManager
import com.ismartcoding.plain.discover.PairingInitiator
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.ensureBlePermissionAsync
import com.ismartcoding.plain.platform.isBluetoothReadyToUse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow

object NearbyViewModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val nearbyDevices = MutableStateFlow<List<DNearbyDevice>>(emptyList())
    var isDiscovering = mutableStateOf(false)
    val itemStatus = mutableStateMapOf<String, NearbyItemStatus>()

    var isBleScanning = mutableStateOf(false)

    private var bleJob: Job? = null
    private var blePermissionJob: Job? = null

    fun startDiscovering() {
        MdnsDiscoverManager.startPeriodicDiscovery()
    }

    fun stopDiscovering() {
        MdnsDiscoverManager.stopPeriodicDiscovery()
    }

    fun requestBlePermission() {
        if (blePermissionJob?.isActive == true) return
        blePermissionJob = scope.launchSafe(
            onError = {
                LogCat.e("BLE permission error: ${it.message}", it)
            },
            block = {
                if (ensureBlePermissionAsync()) {
                    startBleScanning()
                }
            }
        )
    }

    fun startBleScanning() {
        if (isBleScanning.value) return
        if (!isBluetoothReadyToUse()) return
        bleJob = scope.launchSafe(
            onDone = {
                isBleScanning.value = false
                scope.launchSafe { com.ismartcoding.plain.discover.RustNearbyDevices.scanning() }
            },
            onError = {
                LogCat.e("BLE scan error: ${it.message}", it)
                isBleScanning.value = false
            },
            block = {
                isBleScanning.value = true
                scope.launchSafe { com.ismartcoding.plain.discover.RustNearbyDevices.scanning() }
                PairingTransport.scanAndDiscover().collect { device ->
                    NearbyViewModel.handleNewDevice(device)
                }
            }
        )
    }

    fun stopBleScanning() {
        isBleScanning.value = false
        bleJob?.cancel()
        bleJob = null
        scope.launchSafe { com.ismartcoding.plain.discover.RustNearbyDevices.scanning() }
    }

    fun startPairing(device: DNearbyDevice) {
        if (itemStatus[device.id] != null) return
        itemStatus[device.id] = NearbyItemStatus.STARTING

        scope.launchSafe { PairingInitiator.start(device) }
    }

    fun unpairDevice(deviceId: String) {
        val current = itemStatus[deviceId]
        if (current == NearbyItemStatus.UNPAIRING || current == NearbyItemStatus.PAIRING) return
        itemStatus[deviceId] = NearbyItemStatus.UNPAIRING
        scope.launchSafe(onDone = {
            itemStatus.remove(deviceId)
        }) {
            PeerManager.markUnpaired(deviceId)
        }
    }

    fun cancelPairing(deviceId: String) {
        itemStatus.remove(deviceId)
        PairingInitiator.cancel(deviceId)
    }

    fun getStatus(deviceId: String, isPaired: Boolean): NearbyItemStatus {
        return when (itemStatus[deviceId]) {
            NearbyItemStatus.UNPAIRING -> NearbyItemStatus.UNPAIRING
            NearbyItemStatus.STARTING -> NearbyItemStatus.STARTING
            NearbyItemStatus.PAIRING -> if (isPaired) NearbyItemStatus.PAIRED else NearbyItemStatus.PAIRING
            else -> if (isPaired) NearbyItemStatus.PAIRED else NearbyItemStatus.UNPAIRED
        }
    }

    suspend fun handleNewDevice(incoming: DNearbyDevice) {
        com.ismartcoding.plain.discover.RustNearbyDevices.seen(incoming)
    }

    fun handlePairingSuccess(deviceId: String) {
        itemStatus[deviceId] = NearbyItemStatus.PAIRED
    }
}
