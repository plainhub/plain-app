package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.features.bluetooth.client.BluetoothUtil
import com.ismartcoding.plain.ble.PairingTransport
import com.ismartcoding.plain.lib.channel.Channel
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.lib.helpers.NetworkHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DQrPairData
import com.ismartcoding.plain.discover.PairingInitiator
import com.ismartcoding.plain.discover.PairingSessionStore
import com.ismartcoding.plain.events.NearbyDeviceFoundEvent
import com.ismartcoding.plain.events.PairingFailedEvent
import com.ismartcoding.plain.events.PairingSuccessEvent
import com.ismartcoding.plain.events.StartNearbyDiscoveryEvent
import com.ismartcoding.plain.events.StartNearbyServiceEvent
import com.ismartcoding.plain.events.StopNearbyDiscoveryEvent
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.helpers.TimeHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class NearbyViewModel : ViewModel() {
    val nearbyDevices = mutableStateListOf<DNearbyDevice>()
    var isDiscovering = mutableStateOf(false)
    val itemStatus = mutableStateMapOf<String, NearbyItemStatus>()

    var isBleScanning = mutableStateOf(false)
    var blePermissionReady = mutableStateOf(BluetoothUtil.isBluetoothReadyToUse())

    internal var eventJob: Job? = null
    private var cleanupJob: Job? = null
    private var bleJob: Job? = null
    private var blePermissionJob: Job? = null
    private val blePairingJobs = mutableMapOf<String, Job>()

    init {
        startEventListening()
    }

    override fun onCleared() {
        super.onCleared()
        eventJob?.cancel()
        cleanupJob?.cancel()
        bleJob?.cancel()
        blePermissionJob?.cancel()
        blePairingJobs.values.forEach { it.cancel() }
        blePairingJobs.clear()
        sendEvent(StopNearbyDiscoveryEvent())
    }

    fun startDiscovering() {
        isDiscovering.value = true
        sendEvent(StartNearbyDiscoveryEvent())
        startDeviceCleanup()
    }

    fun stopDiscovering() {
        isDiscovering.value = false
        sendEvent(StopNearbyDiscoveryEvent())
        stopDeviceCleanup()
    }

    private fun startDeviceCleanup() {
        cleanupJob = viewModelScope.launch {
            while (isDiscovering.value || isBleScanning.value) {
                delay(20000.milliseconds)
                val currentTime = TimeHelper.now()
                nearbyDevices.removeIf { (currentTime - it.lastSeen).inWholeSeconds > 60 }
            }
        }
    }

    private fun stopDeviceCleanup() {
        if (isDiscovering.value || isBleScanning.value) return
        cleanupJob?.cancel()
        cleanupJob = null
    }

    fun requestBlePermission() {
        if (blePermissionJob?.isActive == true) return
        blePermissionJob = launchSafe(
            onError = {
                LogCat.e("BLE permission error: ${it.message}", it)
            },
            block = {
                val granted = BluetoothUtil.ensurePermissionAsync()
                blePermissionReady.value = granted
                if (granted) {
                    startBleScanning()
                }
            }
        )
    }

    fun startBleScanning() {
        if (isBleScanning.value) return
        if (!BluetoothUtil.isBluetoothReadyToUse()) return
        bleJob = launchSafe(
            onError = {
                LogCat.e("BLE scan error: ${it.message}", it)
                isBleScanning.value = false
            },
            block = {
                isBleScanning.value = true
                startDeviceCleanup()
                try {
                    PairingTransport.scanAndDiscover().collect { device ->
                        sendEvent(NearbyDeviceFoundEvent(device))
                    }
                } finally {
                    isBleScanning.value = false
                    stopDeviceCleanup()
                }
            }
        )
    }

    fun stopBleScanning() {
        isBleScanning.value = false
        bleJob?.cancel()
        bleJob = null
        stopDeviceCleanup()
    }

    fun startPairing(device: DNearbyDevice) {
        if (itemStatus[device.id] != null) return
        itemStatus[device.id] = NearbyItemStatus.PAIRING

        if (device.discoveredViaLan && device.ips.isNotEmpty()) {
            launchSafe {
                PairingInitiator.start(device)
                delay(PAIRING_TIMEOUT_MS)
                if (itemStatus[device.id] == NearbyItemStatus.PAIRING) {
                    PairingSessionStore.remove(device.id)
                    itemStatus.remove(device.id)
                }
            }
        } else if (device.discoveredViaBle && device.bleClient != null) {
            blePairingJobs[device.id] = launchSafe {
                PairingTransport.pairViaBle(device)
                blePairingJobs.remove(device.id)
            }
        } else {
            itemStatus.remove(device.id)
        }
    }

    fun unpairDevice(deviceId: String) {
        if (itemStatus[deviceId] != null) return
        itemStatus[deviceId] = NearbyItemStatus.UNPAIRING
        launchSafe {
            try {
                PeerManager.markUnpaired(deviceId)
            } finally {
                itemStatus.remove(deviceId)
            }
        }
    }

    fun cancelPairing(deviceId: String) {
        itemStatus.remove(deviceId)
        blePairingJobs.remove(deviceId)?.let {
            it.cancel()
            return
        }
        PairingInitiator.cancel(deviceId)
    }

    suspend fun getQrDataAsync(): DQrPairData = withIO {
        val context = MainApp.instance
        val allIps = NetworkHelper.getDeviceIP4s().toList()
        DQrPairData(
            id = TempData.clientId,
            name = TempData.deviceName.value,
            port = TempData.httpsPort.value,
            deviceType = PhoneHelper.getDeviceType(context),
            ips = allIps,
        )
    }

    fun getStatus(deviceId: String, isPaired: Boolean): NearbyItemStatus {
        return when (itemStatus[deviceId]) {
            NearbyItemStatus.UNPAIRING -> NearbyItemStatus.UNPAIRING
            NearbyItemStatus.PAIRING -> if (isPaired) NearbyItemStatus.COMPLETING else NearbyItemStatus.PAIRING
            NearbyItemStatus.COMPLETING -> NearbyItemStatus.COMPLETING
            else -> if (isPaired) NearbyItemStatus.PAIRED else NearbyItemStatus.UNPAIRED
        }
    }

    fun clearStatus(deviceId: String) {
        itemStatus.remove(deviceId)
    }

    private fun startEventListening() {
        eventJob = viewModelScope.launch {
            Channel.sharedFlow.collect { event ->
                when (event) {
                    is NearbyDeviceFoundEvent -> {
                        val incoming = event.device
                        val existingIndex = nearbyDevices.indexOfFirst { it.id == incoming.id }
                        if (existingIndex >= 0) {
                            val existing = nearbyDevices[existingIndex]
                            val merged = incoming.copy(
                                discoveredViaLan = existing.discoveredViaLan || incoming.discoveredViaLan,
                                discoveredViaBle = existing.discoveredViaBle || incoming.discoveredViaBle,
                                bleClient = incoming.bleClient ?: existing.bleClient,
                                ips = (existing.ips + incoming.ips).distinct(),
                                lastSeen = maxOf(existing.lastSeen, incoming.lastSeen),
                            )
                            nearbyDevices[existingIndex] = merged
                        } else {
                            nearbyDevices.add(incoming)
                        }
                    }

                    is PairingFailedEvent -> {
                        itemStatus.remove(event.deviceId)
                    }

                    is PairingSuccessEvent -> {
                        itemStatus[event.deviceId] = NearbyItemStatus.COMPLETING
                    }
                }
            }
        }
    }
}

private const val PAIRING_TIMEOUT_MS = 90_000L
