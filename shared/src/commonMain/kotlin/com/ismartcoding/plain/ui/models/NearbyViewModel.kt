package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.ble.PairingTransport
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DQrPairData
import com.ismartcoding.plain.discover.LANDiscoverManager
import com.ismartcoding.plain.discover.PairingInitiator
import com.ismartcoding.plain.enums.DiscoveryMethod
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.ensureBlePermissionAsync
import com.ismartcoding.plain.platform.getDeviceIP4s
import com.ismartcoding.plain.platform.getDeviceType
import com.ismartcoding.plain.platform.isBluetoothReadyToUse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

object NearbyViewModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val nearbyDevices = mutableStateListOf<DNearbyDevice>()
    var isDiscovering = mutableStateOf(false)
    val itemStatus = mutableStateMapOf<String, NearbyItemStatus>()

    var isBleScanning = mutableStateOf(false)
    var blePermissionReady = mutableStateOf(isBluetoothReadyToUse())

    internal var eventJob: Job? = null
    private var cleanupJob: Job? = null
    private var bleJob: Job? = null
    private var blePermissionJob: Job? = null
    private val blePairingJobs = mutableMapOf<String, Job>()
    private val lastDeviceEventTimes = mutableMapOf<String, Long>()

    fun startDiscovering() {
        isDiscovering.value = true
        LANDiscoverManager.startPeriodicDiscovery()
        startDeviceCleanup()
    }

    fun stopDiscovering() {
        isDiscovering.value = false
        LANDiscoverManager.stopPeriodicDiscovery()
        stopDeviceCleanup()
    }

    private fun startDeviceCleanup() {
        cleanupJob = scope.launch {
            while (isDiscovering.value || isBleScanning.value) {
                delay(20000.milliseconds)
                val currentTime = TimeHelper.now()
                val toRemove = nearbyDevices.filter { (currentTime - it.lastSeen).inWholeSeconds > 60 }
                nearbyDevices.removeAll(toRemove)
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
        blePermissionJob = scope.launchSafe(
            onError = {
                LogCat.e("BLE permission error: ${it.message}", it)
            },
            block = {
                val granted = ensureBlePermissionAsync()
                blePermissionReady.value = granted
                if (granted) {
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
                stopDeviceCleanup()
            },
            onError = {
                LogCat.e("BLE scan error: ${it.message}", it)
                isBleScanning.value = false
            },
            block = {
                isBleScanning.value = true
                startDeviceCleanup()
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
        stopDeviceCleanup()
    }

    fun startPairing(device: DNearbyDevice) {
        if (itemStatus[device.id] != null) return
        itemStatus[device.id] = NearbyItemStatus.PAIRING

        if (DiscoveryMethod.LAN in device.discoveryMethods && device.ips.isNotEmpty()) {
            scope.launchSafe {
                PairingInitiator.start(device)
            }
        } else if (DiscoveryMethod.BLE in device.discoveryMethods && device.bleClient != null) {
            blePairingJobs[device.id] = scope.launchSafe(onDone = {
                blePairingJobs.remove(device.id)
            }) {
                PairingTransport.pairViaBle(device)
            }
        } else {
            itemStatus.remove(device.id)
        }
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
        blePairingJobs.remove(deviceId)?.let {
            it.cancel()
            return
        }
        PairingInitiator.cancel(deviceId)
    }

    suspend fun getQrDataAsync(): DQrPairData = withIO {
        val allIps = getDeviceIP4s()
        DQrPairData(
            id = TempData.clientId,
            name = TempData.deviceName.value,
            port = TempData.httpsPort.value,
            deviceType = getDeviceType(),
            ips = allIps,
        )
    }

    fun getStatus(deviceId: String, isPaired: Boolean): NearbyItemStatus {
        return when (itemStatus[deviceId]) {
            NearbyItemStatus.UNPAIRING -> NearbyItemStatus.UNPAIRING
            NearbyItemStatus.PAIRING -> if (isPaired) NearbyItemStatus.PAIRED else NearbyItemStatus.PAIRING
            else -> if (isPaired) NearbyItemStatus.PAIRED else NearbyItemStatus.UNPAIRED
        }
    }

    fun handleNewDevice(incoming: DNearbyDevice) {
        val existingIndex = nearbyDevices.indexOfFirst { it.id == incoming.id }
        val paired = PeerCacher.pairedPeers.value.any { it.id == incoming.id }
        val now = TimeHelper.nowMillis()
        val shouldSendEvent = (now - (lastDeviceEventTimes[incoming.id] ?: 0L)) >= 1000L
        if (shouldSendEvent) {
            lastDeviceEventTimes[incoming.id] = now
        }
        if (existingIndex >= 0) {
            val existing = nearbyDevices[existingIndex]
            val merged = incoming.copy(
                discoveryMethods = existing.discoveryMethods + incoming.discoveryMethods,
                bleClient = incoming.bleClient ?: existing.bleClient,
                ips = (existing.ips + incoming.ips).distinct(),
                lastSeen = maxOf(existing.lastSeen, incoming.lastSeen),
                status = getStatus(incoming.id, paired)
            )
            nearbyDevices[existingIndex] = merged
            if (shouldSendEvent) {
                sendEvent(WebSocketEvent(EventType.NEARBY_DEVICE_FOUND, JsonHelper.jsonEncode(merged)))
            }
        } else {
            val withStatus = incoming.copy(status = getStatus(incoming.id, paired))
            sendEvent(WebSocketEvent(EventType.NEARBY_DEVICE_FOUND, JsonHelper.jsonEncode(withStatus)))
            nearbyDevices.add(withStatus)
        }
    }

    fun handlePairingSuccess(deviceId: String) {
        itemStatus[deviceId] = NearbyItemStatus.PAIRED
    }
}
