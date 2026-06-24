package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.chat.peer.PeerStatusManager
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.events.HMessageCreatedEvent
import com.ismartcoding.plain.events.NearbyDeviceFoundEvent
import com.ismartcoding.plain.events.PeerOnlineStatusChangedEvent
import com.ismartcoding.plain.events.PeerUnpairedEvent
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference
import kotlinx.coroutines.launch
import kotlin.time.Instant

class PeerViewModel : ViewModel() {
    val pairedPeers = mutableStateListOf<DPeer>()
    val unpairedPeers = mutableStateListOf<DPeer>()
    private val latestChatMap = mutableStateMapOf<String, DChat>()
    val onlineMap = mutableStateMapOf<String, Boolean>()
    val onlinePeerIds = mutableStateSetOf<String>()

    init {
        viewModelScope.launch {
            Channel.sharedFlow.collect { event ->
                when (event) {
                    is HMessageCreatedEvent -> loadPeers()
                    is NearbyDeviceFoundEvent -> handleDeviceFound(event)
                    is PeerOnlineStatusChangedEvent -> {
                        updatePeerOnlineStatus(event.peerId, event.online)
                        if (event.online) onlinePeerIds.add(event.peerId) else onlinePeerIds.remove(event.peerId)
                    }
                    is PeerUnpairedEvent -> loadPeers()
                }
            }
        }
    }

    fun loadPeers() {
        launchSafe {
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

            latestChatMap.clear()
            latestChatMap.putAll(chatCache)
            pairedPeers.clear()
            pairedPeers.addAll(newPairedPeers)
            unpairedPeers.clear()
            unpairedPeers.addAll(newUnpairedPeers)
            syncPeerOnlineStatuses()
        }
    }

    fun getLatestChat(chatId: String): DChat? = latestChatMap[chatId]

    fun updateDiscoverable(discoverable: Boolean) {
        launchSafe {
            NearbyDiscoverablePreference.putAsync(discoverable)
            TempData.nearbyDiscoverable = discoverable
        }
    }

    fun removePeer(context: Context, peerId: String) {
        launchSafe {
            try {
                PeerManager.deletePeer(context, peerId)
                loadPeers()
            } catch (_: Exception) {
            }
        }
    }

    fun updatePeerOnlineStatus(peerId: String, online: Boolean) {
        launchSafe {
            if (onlineMap[peerId] == online) return@launchSafe
            onlineMap[peerId] = online
            resortPairedPeers()
        }
    }

    fun syncPeerOnlineStatuses() {
        launchSafe {
            onlineMap.clear()
            onlineMap.putAll(pairedPeers.associate { it.id to PeerStatusManager.isOnline(it.id) })
            resortPairedPeers()
        }
    }

    fun isPeerOnline(peerId: String): Boolean {
        return onlineMap[peerId] == true
    }

    fun getPeerOnlineStatus(peerId: String): Boolean? {
        return onlineMap[peerId] ?: false
    }

    internal fun resortPairedPeers() {
        val sortedPeers = sortPeersForChatList(pairedPeers.toList(), latestChatMap)
        pairedPeers.clear()
        pairedPeers.addAll(sortedPeers)
    }

    private fun sortPeersForChatList(
        peers: List<DPeer>,
        chatCache: Map<String, DChat>,
    ): List<DPeer> {
        return peers.sortedWith(
            compareByDescending<DPeer> { chatCache[it.id]?.createdAt ?: Instant.DISTANT_PAST }
                .thenByDescending { onlineMap[it.id] == true }
                .thenByDescending { it.createdAt }
                .thenBy { it.name.lowercase() },
        )
    }

    internal fun handleDeviceFound(event: NearbyDeviceFoundEvent) {
        launchSafe {
            val device = event.device
            val updated = PeerManager.applyDeviceDiscovered(
                deviceId = device.id,
                ips = device.ips,
                port = device.port,
                name = device.name,
                deviceType = device.deviceType,
            )
            PeerStatusManager.setOnline(peerId = device.id, true)
            if (updated?.status == "paired") {
                loadPeers()
            }
        }
    }
}
