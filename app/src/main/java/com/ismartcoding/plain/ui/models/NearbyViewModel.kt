package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.features.bluetooth.BlePairingCandidate
import com.ismartcoding.plain.features.bluetooth.BluetoothUtil
import com.ismartcoding.plain.features.bluetooth.PairingTransport
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
import com.ismartcoding.plain.discover.NearbyPairing
import com.ismartcoding.plain.events.NearbyDeviceFoundEvent
import com.ismartcoding.plain.events.PairingFailedEvent
import com.ismartcoding.plain.events.StartNearbyDiscoveryEvent
import com.ismartcoding.plain.events.StopNearbyDiscoveryEvent
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.helpers.TimeHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NearbyViewModel : ViewModel() {
    val nearbyDevices = mutableStateListOf<DNearbyDevice>()
    var isDiscovering = mutableStateOf(false)
    val pairingInProgress = mutableStateListOf<String>()

    val bleCandidates = mutableStateListOf<BlePairingCandidate>()
    var isBleScanning = mutableStateOf(false)
    val isBlePairing = mutableStateListOf<String>()
    val bleErrors = mutableStateOf<String?>(null)

    internal var eventJob: Job? = null
    private var cleanupJob: Job? = null
    private var bleJob: Job? = null

    init {
        startEventListening()
    }

    override fun onCleared() {
        super.onCleared()
        eventJob?.cancel()
        cleanupJob?.cancel()
        bleJob?.cancel()
        sendEvent(StopNearbyDiscoveryEvent())
        PairingTransport.stopAdvertising()
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
            while (isDiscovering.value) {
                delay(20000)
                val currentTime = TimeHelper.now()
                nearbyDevices.removeIf { (currentTime - it.lastSeen).inWholeSeconds > 60 }
            }
        }
    }

    private fun stopDeviceCleanup() {
        cleanupJob?.cancel()
        cleanupJob = null
    }

    fun startBleScanning() {
        LogCat.d("NearbyViewModel.startBleScanning called")
        if (isBleScanning.value) return
        bleJob = launchSafe(
            onError = {
                LogCat.e("BLE scan error: ${it.message}", it)
                bleErrors.value = it.message
                isBleScanning.value = false
            },
            block = {
                LogCat.d("BLE scan: requesting permission")
                val granted = BluetoothUtil.ensurePermissionAsync()
                LogCat.d("BLE scan: permission granted=$granted")
                if (!granted) {
                    LogCat.d("BLE permission not granted, skipping")
                    isBleScanning.value = false
                    return@launchSafe
                }
                LogCat.d("BLE scan: startAdvertising")
                PairingTransport.startAdvertising()
                LogCat.d("BLE scan: advertising started, scanning...")
                isBleScanning.value = true
                try {
                    PairingTransport.scanForPairingCandidates().collect { candidate ->
                        LogCat.d("BLE scan: found ${candidate.mac} ${candidate.name}")
                        if (bleCandidates.none { it.mac == candidate.mac }) {
                            bleCandidates.add(candidate)
                        }
                    }
                } finally {
                    PairingTransport.stopAdvertising()
                    isBleScanning.value = false
                }
            }
        )
    }

    fun stopBleScanning() {
        isBleScanning.value = false
        bleJob?.cancel()
        bleJob = null
        PairingTransport.stopAdvertising()
    }

    fun pairViaBle(candidate: BlePairingCandidate) {
        if (isBlePairing.contains(candidate.mac)) return
        isBlePairing.add(candidate.mac)
        launchSafe {
            val ok = PairingTransport.pairViaBle(candidate)
            isBlePairing.remove(candidate.mac)
            if (ok) {
                bleCandidates.removeAll { it.mac == candidate.mac }
                PeerManager.load()
            }
        }
    }

    fun startPairing(device: DNearbyDevice) {
        pairingInProgress.add(device.id)
        startPairingDevice(device)
    }

    fun unpairDevice(deviceId: String) {
        launchSafe {
            PeerManager.markUnpaired(deviceId)
        }
    }

    fun cancelPairing(deviceId: String) {
        pairingInProgress.removeIf { it == deviceId }
        NearbyPairing.cancelPairing(deviceId)
    }

    private fun startPairingDevice(device: DNearbyDevice) {
        launchSafe {
            NearbyPairing.startPairingAsync(device)
        }
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

    fun isPairing(deviceId: String): Boolean {
        return pairingInProgress.contains(deviceId)
    }

    fun isBleCandidatePairing(mac: String): Boolean = isBlePairing.contains(mac)

    private fun startEventListening() {
        eventJob = viewModelScope.launch {
            Channel.sharedFlow.collect { event ->
                when (event) {
                    is NearbyDeviceFoundEvent -> {
                        val existingIndex = nearbyDevices.indexOfFirst { it.id == event.device.id }
                        if (existingIndex >= 0) {
                            nearbyDevices[existingIndex] = event.device
                        } else {
                            nearbyDevices.add(event.device)
                        }
                    }

                    is PairingFailedEvent -> {
                        pairingInProgress.removeIf { it == event.deviceId }
                    }
                }
            }
        }
    }
}
