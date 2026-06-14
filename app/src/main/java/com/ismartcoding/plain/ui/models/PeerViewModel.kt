package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.plain.chat.PeerManager
import com.ismartcoding.plain.chat.PeerStatusManager
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.events.NearbyDeviceFoundEvent
import com.ismartcoding.plain.events.PeerOnlineStatusChangedEvent
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.events.HMessageCreatedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PeerViewModel : ViewModel() {
    val pairedPeers = mutableStateListOf<DPeer>()
    val unpairedPeers = mutableStateListOf<DPeer>()
    internal val latestChatCacheInternal = mutableStateMapOf<String, DChat>()
    val onlineMap = mutableStateOf<Map<String, Boolean>>(emptyMap())
    private var eventJob: Job? = null

    init { startEventListening() }

    private fun startEventListening() {
        eventJob = viewModelScope.launch {
            Channel.sharedFlow.collect { event ->
                when (event) {
                    is HMessageCreatedEvent -> viewModelScope.launch { loadPeers() }
                    is NearbyDeviceFoundEvent -> handleDeviceFoundInternal(event)
                    is PeerOnlineStatusChangedEvent -> updatePeerOnlineStatus(event.peerId, event.online)
                }
            }
        }
    }

    override fun onCleared() { super.onCleared(); eventJob?.cancel() }

    fun loadPeers() = loadPeersInternal()

    fun getLatestChat(chatId: String): DChat? = latestChatCacheInternal[chatId]

    fun updateDiscoverable(context: Context, discoverable: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            NearbyDiscoverablePreference.putAsync(discoverable)
            TempData.nearbyDiscoverable = discoverable
        }
    }

    fun removePeer(context: Context, peerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                PeerManager.deletePeer(context, peerId)
                loadPeers()
            } catch (_: Exception) {}
        }
    }

    fun updatePeerOnlineStatus(peerId: String, online: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            if (onlineMap.value[peerId] == online) return@launch

            val currentMap = onlineMap.value.toMutableMap()
            currentMap[peerId] = online
            onlineMap.value = currentMap
            resortPairedPeersInternal()
        }
    }

    fun syncPeerOnlineStatuses() {
        viewModelScope.launch(Dispatchers.Main) {
            onlineMap.value = pairedPeers.associate { it.id to PeerStatusManager.isOnline(it.id) }
            resortPairedPeersInternal()
        }
    }

    fun isPeerOnline(peerId: String): Boolean {
        return onlineMap.value[peerId] == true
    }

    fun getPeerOnlineStatus(peerId: String): Boolean? {
        return onlineMap.value[peerId] ?: false
    }
}
