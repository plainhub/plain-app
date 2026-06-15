package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.chat.channel.ChannelSystemMessageSender
import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.chat.peer.PeerStatusManager
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.events.HMessageCreatedEvent
import com.ismartcoding.plain.events.NearbyDeviceFoundEvent
import com.ismartcoding.plain.events.PeerOnlineStatusChangedEvent
import com.ismartcoding.plain.events.PeerUpdatedEvent
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Instant

class PeerViewModel : ViewModel() {
    val pairedPeers = mutableStateListOf<DPeer>()
    val unpairedPeers = mutableStateListOf<DPeer>()
    internal val latestChatCacheInternal = mutableStateMapOf<String, DChat>()
    val onlineMap = mutableStateOf<Map<String, Boolean>>(emptyMap())
    private var eventJob: Job? = null

    init {
        startEventListening()
    }

    private fun startEventListening() {
        eventJob = viewModelScope.launch {
            Channel.sharedFlow.collect { event ->
                when (event) {
                    is HMessageCreatedEvent -> loadPeers()
                    is NearbyDeviceFoundEvent -> handleDeviceFound(event)
                    is PeerOnlineStatusChangedEvent -> updatePeerOnlineStatus(event.peerId, event.online)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared(); eventJob?.cancel()
    }

    fun loadPeers() {
        viewModelScope.launch(Dispatchers.IO) {
            val allPeers = AppDatabase.instance.peerDao().getAll()
            val allChannels = AppDatabase.instance.chatChannelDao().getAll()
            val chatDao = AppDatabase.instance.chatDao()
            val chatCache = mutableMapOf<String, DChat>()
            val latestChats = chatDao.getAllLatestChats()
            val peerIds = allPeers.map { it.id }.toSet()
            val channelIds = allChannels.map { it.id }.toSet()

            latestChats.forEach { chat ->
                val chatId = when {
                    chat.channelId.isNotEmpty() && channelIds.contains(chat.channelId) -> chat.channelId
                    (chat.fromId == "me" && chat.toId == "local") || (chat.fromId == "local" && chat.toId == "me") -> "local"
                    chat.fromId == "me" && peerIds.contains(chat.toId) -> chat.toId
                    chat.toId == "me" && peerIds.contains(chat.fromId) -> chat.fromId
                    else -> null
                }
                if (chatId != null) {
                    val existing = chatCache[chatId]
                    if (existing == null || chat.createdAt > existing.createdAt) chatCache[chatId] = chat
                }
            }

            val newPairedPeers = allPeers.filter { it.status == "paired" }
            val newUnpairedPeers = sortPeersForChatList(allPeers.filter { it.status == "unpaired" }, chatCache)
            ChatCacheManager.refreshPeerMap(allPeers)

            latestChatCacheInternal.clear()
            latestChatCacheInternal.putAll(chatCache)
            pairedPeers.clear()
            pairedPeers.addAll(newPairedPeers)
            unpairedPeers.clear()
            unpairedPeers.addAll(newUnpairedPeers)
            syncPeerOnlineStatuses()
        }
    }

    fun getLatestChat(chatId: String): DChat? = latestChatCacheInternal[chatId]

    fun updateDiscoverable(discoverable: Boolean) {
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
            } catch (_: Exception) {
            }
        }
    }

    fun updatePeerOnlineStatus(peerId: String, online: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            if (onlineMap.value[peerId] == online) return@launch

            val currentMap = onlineMap.value.toMutableMap()
            currentMap[peerId] = online
            onlineMap.value = currentMap
            resortPairedPeers()
        }
    }

    fun syncPeerOnlineStatuses() {
        viewModelScope.launch(Dispatchers.Main) {
            onlineMap.value = pairedPeers.associate { it.id to PeerStatusManager.isOnline(it.id) }
            resortPairedPeers()
        }
    }

    fun isPeerOnline(peerId: String): Boolean {
        return onlineMap.value[peerId] == true
    }

    fun getPeerOnlineStatus(peerId: String): Boolean? {
        return onlineMap.value[peerId] ?: false
    }

    internal fun resortPairedPeers() {
        val sortedPeers = sortPeersForChatList(pairedPeers.toList(), latestChatCacheInternal)
        pairedPeers.clear()
        pairedPeers.addAll(sortedPeers)
    }

    private fun sortPeersForChatList(
        peers: List<DPeer>,
        chatCache: Map<String, DChat>,
    ): List<DPeer> {
        return peers.sortedWith(
            compareByDescending<DPeer> { chatCache[it.id]?.createdAt ?: Instant.DISTANT_PAST }
                .thenByDescending { onlineMap.value[it.id] == true }
                .thenByDescending { it.createdAt }
                .thenBy { it.name.lowercase() },
        )
    }

    internal fun handleDeviceFound(event: NearbyDeviceFoundEvent) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val device = event.device
                val updated = PeerManager.applyDeviceDiscovered(
                    deviceId = device.id,
                    ips = device.ips,
                    port = device.port,
                    name = device.name,
                    deviceType = device.deviceType,
                )
                val existing = updated ?: AppDatabase.instance.peerDao().getById(device.id)
                if (existing != null && existing.status == "paired") {
                    if (updated != null) {
                        loadPeers()
                        sendEvent(PeerUpdatedEvent(updated))
                    }
                    retryPendingChannelInvites(existing)
                }
                PeerStatusManager.setOnline(peerId = device.id, true)
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun retryPendingChannelInvites(peer: DPeer) {
        try {
            val channels = AppDatabase.instance.chatChannelDao().getOwnedChannels()
            channels.filter { ch -> ch.findMember(peer.id)?.isPending() == true }
                .forEach { channel -> ChannelSystemMessageSender.sendInvite(channel, peer) }
        } catch (_: Exception) {
        }
    }
}
