package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.ble.PairingTransport
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.discover.MdnsDiscoverManager
import com.ismartcoding.plain.discover.NearbyDeviceCache
import com.ismartcoding.plain.discover.NearbyHttpClient
import com.ismartcoding.plain.discover.PairingInitiator
import com.ismartcoding.plain.enums.DiscoveryMethod
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.bleTransport
import com.ismartcoding.plain.platform.ensureBlePermissionAsync
import com.ismartcoding.plain.platform.getBestIp
import com.ismartcoding.plain.platform.isBluetoothReadyToUse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

object NearbyViewModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val nearbyDevices = MutableStateFlow<List<DNearbyDevice>>(emptyList())
    var isDiscovering = mutableStateOf(false)
    val itemStatus = mutableStateMapOf<String, NearbyItemStatus>()

    var isBleScanning = mutableStateOf(false)

    private var cleanupJob: Job? = null
    private var bleJob: Job? = null
    private var blePermissionJob: Job? = null
    private val blePairingJobs = mutableStateMapOf<String, Job>()
    private val lastDeviceEventTimes = HashMap<String, Long>()
    private val deviceListMutex = Mutex()

    fun startDiscovering() {
        isDiscovering.value = true
        loadCachedDevices()
        MdnsDiscoverManager.startPeriodicDiscovery()
        startDeviceCleanup()
    }

    fun stopDiscovering() {
        isDiscovering.value = false
        MdnsDiscoverManager.stopPeriodicDiscovery()
        stopDeviceCleanup()
    }

    /**
     * Renders the persisted discovery history instantly, so the page opens
     * with the last known LAN instead of an empty scan. Stale entries are
     * then verified by [sweepStaleDevices]; live ones refresh via mDNS.
     */
    private fun loadCachedDevices() {
        if (nearbyDevices.value.isNotEmpty()) return
        scope.launch {
            val cached = runCatching { NearbyDeviceCache.getAllAsync() }.getOrElse {
                LogCat.e("NearbyDeviceCache load failed: ${it.message}", it)
                return@launch
            }
            if (cached.isEmpty()) return@launch
            val filled = deviceListMutex.withLock {
                if (nearbyDevices.value.isNotEmpty()) return@withLock false
                val pairedIds = PeerCacher.pairedPeers.value.map { it.id }.toSet()
                nearbyDevices.value = cached.map { it.copy(status = getStatus(it.id, it.id in pairedIds)) }
                true
            }
            if (filled) sweepStaleDevices()
        }
    }

    private fun startDeviceCleanup() {
        cleanupJob = scope.launch {
            val scanner = bleTransport().createScanner()
            while (isDiscovering.value || isBleScanning.value) {
                delay(20000.milliseconds)
                // BLE devices only refresh lastSeen from scan results. While
                // the scan radio is paused for a GATT session (e.g. pairing),
                // skipping this sweep prevents every BLE device from being
                // timed out and cleared.
                if (scanner.isScanPaused()) continue
                sweepStaleDevices()
            }
        }
    }

    private fun stopDeviceCleanup() {
        if (isDiscovering.value || isBleScanning.value) return
        cleanupJob?.cancel()
        cleanupJob = null
    }

    /**
     * Handles devices not seen for over a minute. A LAN-reachable device is
     * probed over `/nearby` first — a peer that stopped announcing (screen
     * off, dropped multicast) but is still on the LAN stays listed; only a
     * confirmed-absent device is dropped from the list and the history
     * cache. BLE-only devices have no probe endpoint and keep the plain
     * timeout removal.
     */
    private suspend fun sweepStaleDevices() {
        val now = TimeHelper.now()
        val stale = deviceListMutex.withLock {
            nearbyDevices.value.filter { (now - it.lastSeen).inWholeSeconds > 60 }
        }
        if (stale.isEmpty()) return
        val probed = stale.map { device ->
            scope.async {
                val lanReachable = DiscoveryMethod.LAN in device.discoveryMethods && device.ips.isNotEmpty()
                val alive = lanReachable && NearbyHttpClient.probe(getBestIp(device.ips), device.port)
                device.id to alive
            }
        }.awaitAll()

        val deadIds = probed.filter { !it.second }.map { it.first }.toSet()
        val aliveIds = probed.filter { it.second }.map { it.first }
        val verifiedAt = TimeHelper.now()
        deviceListMutex.withLock {
            if (deadIds.isEmpty() && aliveIds.isEmpty()) return@withLock
            nearbyDevices.value = nearbyDevices.value.mapNotNull { d ->
                when {
                    d.id in deadIds -> null
                    d.id in aliveIds -> d.copy(lastSeen = verifiedAt)
                    else -> d
                }
            }
        }
        // Cache maintenance mirrors the in-memory verdict so the next page
        // open does not resurrect devices that already left the LAN.
        aliveIds.forEach { id -> runCatching { NearbyDeviceCache.touchAsync(id) } }
        deadIds.forEach { id -> runCatching { NearbyDeviceCache.removeAsync(id) } }
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
        itemStatus[device.id] = NearbyItemStatus.STARTING

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

    /**
     * The pairing request has been sent successfully. Transition from the
     * STARTING (loading) state to PAIRING (pending) so the user knows the
     * request reached the peer and we are waiting for its response.
     */
    fun onPairingRequestSent(deviceId: String) {
        if (itemStatus[deviceId] == NearbyItemStatus.STARTING) {
            itemStatus[deviceId] = NearbyItemStatus.PAIRING
        }
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
        deviceListMutex.withLock {
            val current = nearbyDevices.value
            val paired = PeerCacher.pairedPeers.value.any { it.id == incoming.id }
            val now = TimeHelper.nowMillis()
            val shouldSendEvent = (now - (lastDeviceEventTimes[incoming.id] ?: 0L)) >= 1000L
            if (shouldSendEvent) {
                lastDeviceEventTimes[incoming.id] = now
            }
            val existingIndex = current.indexOfFirst { it.id == incoming.id }
            if (existingIndex >= 0) {
                val existing = current[existingIndex]
                val merged = incoming.copy(
                    discoveryMethods = existing.discoveryMethods + incoming.discoveryMethods,
                    bleClient = incoming.bleClient ?: existing.bleClient,
                    ips = (existing.ips + incoming.ips).distinct(),
                    lastSeen = maxOf(existing.lastSeen, incoming.lastSeen),
                    status = getStatus(incoming.id, paired)
                )
                if (shouldSendEvent) {
                    sendEvent(WebSocketEvent(EventType.NEARBY_DEVICE_FOUND, JsonHelper.jsonEncode(merged)))
                }
                nearbyDevices.value = current.mapIndexed { index, device ->
                    if (index == existingIndex) merged else device
                }
            } else {
                val withStatus = incoming.copy(status = getStatus(incoming.id, paired))
                sendEvent(WebSocketEvent(EventType.NEARBY_DEVICE_FOUND, JsonHelper.jsonEncode(withStatus)))
                nearbyDevices.value = current + withStatus
            }
        }
    }

    fun handlePairingSuccess(deviceId: String) {
        itemStatus[deviceId] = NearbyItemStatus.PAIRED
    }
}
