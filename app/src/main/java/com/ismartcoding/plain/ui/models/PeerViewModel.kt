package com.ismartcoding.plain.ui.models
import com.ismartcoding.plain.preferences.*

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.chat.PeerStatusManager
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.events.HttpApiEvents
import com.ismartcoding.plain.events.NearbyDeviceFoundEvent
import com.ismartcoding.plain.events.PeerOnlineStatusChangedEvent
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference
import com.ismartcoding.plain.TempData
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
                    is HttpApiEvents.MessageCreatedEvent -> viewModelScope.launch { loadPeers() }
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
                ChatDbHelper.deleteAllChatsByPeerAsync(context, peerId)
                val isChannelMember = AppDatabase.instance.chatChannelDao().getAll().any { it.hasMember(peerId) }
                val peerDao = AppDatabase.instance.peerDao()
                if (isChannelMember) {
                    val peer = peerDao.getById(peerId)
                    if (peer != null) { peer.key = ""
                        peer.status = "channel"
                        peerDao.update(peer) }
                } else {
                    peerDao.delete(peerId)
                }
                ChatCacheManager.loadKeyCacheAsync()
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
