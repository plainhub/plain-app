package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DQrPairData
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.discover.NearbyPairing
import com.ismartcoding.plain.events.NearbyDeviceFoundEvent
import com.ismartcoding.plain.events.PairingFailedEvent
import com.ismartcoding.plain.events.PairingSuccessEvent
import com.ismartcoding.plain.events.StartNearbyDiscoveryEvent
import com.ismartcoding.plain.events.StopNearbyDiscoveryEvent
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.helpers.TimeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NearbyViewModel : ViewModel() {
    val nearbyDevices = mutableStateListOf<DNearbyDevice>()
    val pairedDevices = mutableStateListOf<DPeer>()
    var isDiscovering = mutableStateOf(false)
    val pairingInProgress = mutableStateListOf<String>()

    internal var eventJob: Job? = null
    private var cleanupJob: Job? = null

    init {
        startEventListening()
        loadPairedDevicesAsync()
    }

    override fun onCleared() {
        super.onCleared()
        eventJob?.cancel()
        cleanupJob?.cancel()
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

    fun startPairing(device: DNearbyDevice) {
        pairingInProgress.add(device.id)
        startPairingDevice(device)
    }

    fun unpairDevice(deviceId: String) {
        launchIO {
            try {
                if (PeerManager.markUnpaired(deviceId)) {
                    loadAsync()
                }
            } catch (e: Exception) {
                LogCat.e("Error unpairing device: ${e.message}")
            }
        }
    }

    fun cancelPairing(deviceId: String) {
        pairingInProgress.removeIf { it == deviceId }
        NearbyPairing.cancelPairing(deviceId)
    }

    private fun startPairingDevice(device: DNearbyDevice) {
        launchIO {
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

    fun isPaired(deviceId: String): Boolean {
        return pairedDevices.any { it.id == deviceId && it.status == "paired" }
    }

    fun isPairing(deviceId: String): Boolean {
        return pairingInProgress.contains(deviceId)
    }

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

                    is PairingSuccessEvent -> {
                        pairingInProgress.removeIf { it == event.deviceId }
                        loadPairedDevicesAsync()
                    }

                    is PairingFailedEvent -> {
                        pairingInProgress.removeIf { it == event.deviceId }
                    }
                }
            }
        }
    }

    private fun loadPairedDevicesAsync() {
        launchIO {
            loadAsync()
        }
    }

    internal suspend fun loadAsync() = withIO {
        val peers = AppDatabase.instance.peerDao().getAll()
        pairedDevices.clear()
        pairedDevices.addAll(peers)
    }
}