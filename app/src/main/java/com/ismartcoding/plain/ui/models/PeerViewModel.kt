package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.pinyin.Pinyin
import com.ismartcoding.plain.chat.ChannelSystemMessageSender
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.events.HttpApiEvents
import com.ismartcoding.plain.events.NearbyDeviceFoundEvent
import com.ismartcoding.plain.events.PairingSuccessEvent
import com.ismartcoding.plain.events.PeerUpdatedEvent
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.helpers.TimeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Instant
import kotlin.time.Duration.Companion.seconds

/**
 * Manages peer state (paired/unpaired device list, online status, latest chat
 * message previews) and peer-related mutations.
 *
 * Channel list state and mutations have moved to [ChannelViewModel].
 */
class PeerViewModel : ViewModel() {
    val pairedPeers = mutableStateListOf<DPeer>()
    val unpairedPeers = mutableStateListOf<DPeer>()

    // Cache for latest chat messages: chatId -> DChat
    private val latestChatCache = mutableMapOf<String, DChat>()

    // Last active time cache: peerId -> Instant
    val onlineMap = mutableStateOf<Map<String, Instant>>(emptyMap())

    private var eventJob: Job? = null

    init {
        startEventListening()
    }

    private fun startEventListening() {
        eventJob = viewModelScope.launch {
            Channel.sharedFlow.collect { event ->
                when (event) {
                    is HttpApiEvents.MessageCreatedEvent -> {
                        viewModelScope.launch {
                            loadPeers()
                        }
                    }

                    is NearbyDeviceFoundEvent -> {
                        handleDeviceFound(event)
                    }

                    is PairingSuccessEvent -> {
                        // Both devices are online immediately after a successful pairing
                        updatePeerLastActive(event.deviceId)
                    }


                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        eventJob?.cancel()
    }

    fun loadPeers() {
        viewModelScope.launch(Dispatchers.IO) {
            val allPeers = AppDatabase.instance.peerDao().getAll()
            val allChannels = AppDatabase.instance.chatChannelDao().getAll()
            val chatDao = AppDatabase.instance.chatDao()

            // Load all latest chat messages in one query
            val chatCache = mutableMapOf<String, DChat>()
            val latestChats = chatDao.getAllLatestChats()

            // Build peer ID set and channel ID set for fast lookup
            val peerIds = allPeers.map { it.id }.toSet()
            val channelIds = allChannels.map { it.id }.toSet()

            latestChats.forEach { chat ->
                val chatId = when {
                    // Channel chat: identified by channelId field
                    chat.channelId.isNotEmpty() && channelIds.contains(chat.channelId) -> chat.channelId

                    // Local chat: me <-> local
                    (chat.fromId == "me" && chat.toId == "local") ||
                            (chat.fromId == "local" && chat.toId == "me") -> "local"

                    // Peer chat: me <-> peer_id
                    chat.fromId == "me" && peerIds.contains(chat.toId) -> chat.toId
                    chat.toId == "me" && peerIds.contains(chat.fromId) -> chat.fromId

                    else -> null
                }

                if (chatId != null) {
                    // Keep the most recent one if there are duplicates
                    val existing = chatCache[chatId]
                    if (existing == null || chat.createdAt > existing.createdAt) {
                        chatCache[chatId] = chat
                    }
                }
            }

            // Prepare new lists off the main thread
            val newPairedPeers = allPeers
                .filter { it.status == "paired" }
                .sortedWith(
                    compareByDescending<DPeer> { peer ->
                        chatCache[peer.id]?.createdAt ?: Instant.DISTANT_PAST
                    }.thenBy { Pinyin.toPinyin(it.name) }
                )
            val newUnpairedPeers = allPeers
                .filter { it.status == "unpaired" }
                .sortedBy { Pinyin.toPinyin(it.name) }

            // Refresh peer map cache before switching to main thread
            ChatCacheManager.refreshPeerMap(allPeers)

            // Apply state updates on the main thread to avoid snapshot violations
            withContext(Dispatchers.Main) {
                latestChatCache.clear()
                latestChatCache.putAll(chatCache)

                pairedPeers.clear()
                pairedPeers.addAll(newPairedPeers)

                unpairedPeers.clear()
                unpairedPeers.addAll(newUnpairedPeers)
            }
        }
    }

    fun getLatestChat(chatId: String): DChat? {
        return latestChatCache[chatId]
    }

    fun updateDiscoverable(context: Context, discoverable: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            NearbyDiscoverablePreference.putAsync(context, discoverable)
            TempData.nearbyDiscoverable = discoverable
        }
    }

    fun removePeer(context: Context, peerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Delete all direct (peer-to-peer) chat messages and associated files for this peer
                ChatDbHelper.deleteAllChatsByPeerAsync(context, peerId)

                // Check if the peer is a member of any channel before deleting
                val isChannelMember = AppDatabase.instance.chatChannelDao().getAll()
                    .any { it.hasMember(peerId) }

                val peerDao = AppDatabase.instance.peerDao()
                if (isChannelMember) {
                    // Downgrade the peer to channel-only: clear pairing key but keep the record
                    val peer = peerDao.getById(peerId)
                    if (peer != null) {
                        peer.key = ""
                        peer.status = "channel"
                        peerDao.update(peer)
                    }
                } else {
                    // Not a channel member — safe to delete the peer record entirely
                    peerDao.delete(peerId)
                }

                // Reload key cache and peers list
                ChatCacheManager.loadKeyCacheAsync()
                loadPeers()
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    fun updatePeerLastActive(peerId: String) {
        // Ensure state mutation happens on the main thread
        viewModelScope.launch(Dispatchers.Main) {
            val currentMap = onlineMap.value.toMutableMap()
            currentMap[peerId] = TimeHelper.now()
            onlineMap.value = currentMap
        }
    }

    fun isPeerOnline(peerId: String): Boolean {
        val lastActive = onlineMap.value[peerId] ?: return false
        val now = TimeHelper.now()
        return (now - lastActive) <= 15.seconds
    }

    fun getPeerOnlineStatus(peerId: String): Boolean? {
        return if (onlineMap.value.containsKey(peerId)) isPeerOnline(peerId) else false
    }

    private fun handleDeviceFound(event: NearbyDeviceFoundEvent) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val device = event.device
                // Check if this device is a paired peer
                val peer = AppDatabase.instance.peerDao().getById(device.id)

                if (peer != null && peer.status == "paired") {
                    // Update peer information if anything has changed
                    var needsUpdate = false

                    // Use all advertised IPs from discovery reply
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
                        loadPeers()
                        sendEvent(PeerUpdatedEvent(peer))
                    }

                    // Update last active time for this peer
                    updatePeerLastActive(device.id)

                    // Retry pending channel invites for this peer
                    retryPendingChannelInvites(peer)
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    /**
     * When a paired peer comes online, check if any of our owned channels have
     * pending invites for this peer and re-send them.
     */
    private suspend fun retryPendingChannelInvites(peer: DPeer) {
        try {
            val channels = AppDatabase.instance.chatChannelDao().getOwnedChannels()
            channels.filter { ch -> ch.findMember(peer.id)?.isPending() == true }
                .forEach { channel ->
                    ChannelSystemMessageSender.sendInvite(channel, peer)
                }
        } catch (e: Exception) {
            // Best-effort retry
        }
    }
} 