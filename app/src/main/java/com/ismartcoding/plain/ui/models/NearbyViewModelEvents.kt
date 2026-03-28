package com.ismartcoding.plain.ui.models

import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.chat.discover.NearbyPairManager
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.events.NearbyDeviceFoundEvent
import com.ismartcoding.plain.events.PairingFailedEvent
import com.ismartcoding.plain.events.PairingSuccessEvent
import com.ismartcoding.plain.helpers.TimeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal fun NearbyViewModel.startEventListening() {
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

internal fun NearbyViewModel.loadPairedDevicesAsync() {
    viewModelScope.launch(Dispatchers.IO) {
        loadAsync()
    }
}

internal fun NearbyViewModel.unpairDeviceAsync(deviceId: String) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            val peer = AppDatabase.instance.peerDao().getById(deviceId)
            if (peer != null) {
                peer.status = "unpaired"
                peer.updatedAt = TimeHelper.now()
                AppDatabase.instance.peerDao().update(peer)
                ChatCacheManager.loadKeyCacheAsync()
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

internal fun NearbyViewModel.startPairingDevice(deviceId: String) {
    viewModelScope.launch(Dispatchers.IO) {
        val device = nearbyDevices.find { it.id == deviceId } ?: return@launch
        NearbyPairManager.startPairingAsync(device)
    }
}
