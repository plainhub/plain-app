package com.ismartcoding.plain.ui.models

import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.channel.ChannelSystemMessageSender
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.events.NearbyDeviceFoundEvent
import com.ismartcoding.plain.events.PeerUpdatedEvent
import com.ismartcoding.plain.helpers.TimeHelper
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.chat.PeerStatusManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Instant

internal fun PeerViewModel.loadPeersInternal() {
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
        val newUnpairedPeers = sortPeersForChatListInternal(allPeers.filter { it.status == "unpaired" }, chatCache)
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

internal fun PeerViewModel.resortPairedPeersInternal() {
    val sortedPeers = sortPeersForChatListInternal(pairedPeers.toList(), latestChatCacheInternal)
    pairedPeers.clear()
    pairedPeers.addAll(sortedPeers)
}

private fun PeerViewModel.sortPeersForChatListInternal(
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

internal fun PeerViewModel.handleDeviceFoundInternal(event: NearbyDeviceFoundEvent) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            val device = event.device
            val peer = AppDatabase.instance.peerDao().getById(device.id)
            if (peer != null && peer.status == "paired") {
                var needsUpdate = false
                val newIpString = device.ips.joinToString(",")
                if (peer.ip != newIpString) {
                    peer.ip = newIpString
                    needsUpdate = true
                }
                if (peer.port != device.port) {
                    peer.port = device.port
                    needsUpdate = true
                }
                if (peer.name != device.name) {
                    peer.name = device.name
                    needsUpdate = true
                }
                if (peer.deviceType != device.deviceType.value) {
                    peer.deviceType = device.deviceType.value
                    needsUpdate = true
                }
                if (needsUpdate) {
                    peer.updatedAt = TimeHelper.now()
                    AppDatabase.instance.peerDao().update(peer)
                    loadPeersInternal()
                    sendEvent(PeerUpdatedEvent(peer))
                }
                PeerStatusManager.setOnline(peerId = device.id, true)
                retryPendingChannelInvitesInternal(peer)
            }
        } catch (_: Exception) {
        }
    }
}

internal suspend fun PeerViewModel.retryPendingChannelInvitesInternal(peer: DPeer) {
    try {
        val channels = AppDatabase.instance.chatChannelDao().getOwnedChannels()
        channels.filter { ch -> ch.findMember(peer.id)?.isPending() == true }
            .forEach { channel -> ChannelSystemMessageSender.sendInvite(channel, peer) }
    } catch (_: Exception) {
    }
}
