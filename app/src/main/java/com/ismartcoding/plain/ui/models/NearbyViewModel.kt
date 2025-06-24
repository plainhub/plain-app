package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.events.NearbyDeviceFoundEvent
import com.ismartcoding.plain.events.PairingFailedEvent
import com.ismartcoding.plain.events.PairingSuccessEvent
import com.ismartcoding.plain.events.StartNearbyDiscoveryEvent
import com.ismartcoding.plain.events.StartPairingEvent
import com.ismartcoding.plain.events.StopNearbyDiscoveryEvent
import com.ismartcoding.plain.features.nearby.NearbyPairManager
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference
import com.ismartcoding.plain.web.ChatApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class NearbyViewModel : ViewModel() {
    val nearbyDevices = mutableStateListOf<DNearbyDevice>()
    val pairedDevices = mutableStateListOf<DPeer>()
    var isDiscovering = mutableStateOf(false)
    val pairingInProgress = mutableStateListOf<String>()

    private var eventJob: Job? = null
    private var cleanupJob: Job? = null

    init {
        startEventListening()
        loadPairedDevices()
    }

    private fun startEventListening() {
        eventJob = viewModelScope.launch {
            Channel.sharedFlow.collect { event ->
                when (event) {
                    is NearbyDeviceFoundEvent -> {
                        val existingIndex = nearbyDevices.indexOfFirst { it.ip == event.device.ip }
                        if (existingIndex >= 0) {
                            nearbyDevices[existingIndex] = event.device
                        } else {
                            nearbyDevices.add(event.device)
                        }
                    }

                    is PairingSuccessEvent -> {
                        pairingInProgress.removeIf { it == event.deviceId }
                        loadPairedDevices()
                    }

                    is PairingFailedEvent -> {
                        pairingInProgress.removeIf { it == event.deviceId }
                        // Could handle error message here
                    }
                }
            }
        }
    }

    private fun loadPairedDevices() {
        viewModelScope.launch(Dispatchers.IO) {
            loadAsync()
        }
    }

    private suspend fun loadAsync() {
        val peers = AppDatabase.instance.peerDao().getAll()
        pairedDevices.clear()
        pairedDevices.addAll(peers)
    }

    fun toggleDiscovering() {
        if (isDiscovering.value) {
            stopDiscovering()
        } else {
            startDiscovering()
        }
    }

    private fun startDiscovering() {
        isDiscovering.value = true
        sendEvent(StartNearbyDiscoveryEvent())
        startDeviceCleanup()
    }

    private fun stopDiscovering() {
        isDiscovering.value = false
        sendEvent(StopNearbyDiscoveryEvent())
        stopDeviceCleanup()
    }

    private fun startDeviceCleanup() {
        cleanupJob = viewModelScope.launch {
            while (isDiscovering.value) {
                delay(20000) // Check every 20 seconds
                val currentTime = Clock.System.now()
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
        sendEvent(StartPairingEvent(device))
    }

    fun unpairDevice(deviceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val peer = AppDatabase.instance.peerDao().getById(deviceId)
                if (peer != null) {
                    peer.status = "unpaired"
                    peer.updatedAt = kotlinx.datetime.Clock.System.now()
                    AppDatabase.instance.peerDao().update(peer)
                    ChatApiManager.loadKeyCacheAsync()
                    loadAsync()
                    LogCat.d("Device unpaired: $deviceId")
                } else {
                    LogCat.w("Device not found for unpair: $deviceId")
                }
            } catch (e: Exception) {
                LogCat.e("Error unpairing device: ${e.message}")
            }
        }
    }

    fun cancelPairing(deviceId: String) {
        pairingInProgress.removeIf { it == deviceId }
        NearbyPairManager.cancelPairing(deviceId)
    }

    fun updateDiscoverable(context: Context, discoverable: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            NearbyDiscoverablePreference.putAsync(context, discoverable)
        }
    }

    fun isPaired(deviceId: String): Boolean {
        return pairedDevices.any { it.id == deviceId && it.status == "paired" }
    }

    fun isPairing(deviceId: String): Boolean {
        return pairingInProgress.contains(deviceId)
    }

    override fun onCleared() {
        super.onCleared()
        eventJob?.cancel()
        cleanupJob?.cancel()
        // Stop nearby service and discovery when ViewModel is cleared
        sendEvent(StopNearbyDiscoveryEvent())
    }
} 