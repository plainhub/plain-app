package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.pinyin.Pinyin
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.events.HttpApiEvents
import com.ismartcoding.plain.events.NearbyDeviceFoundEvent
import com.ismartcoding.plain.features.ChatHelper
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference
import com.ismartcoding.plain.web.ChatApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds

class ChatListViewModel : ViewModel() {
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
            try {
                val allPeers = AppDatabase.instance.peerDao().getAll()
                val chatDao = AppDatabase.instance.chatDao()

                // Load all latest chat messages in one query
                val chatCache = mutableMapOf<String, DChat>()
                val latestChats = chatDao.getAllLatestChats()

                // Build peer ID set for fast lookup
                val peerIds = allPeers.map { it.id }.toSet()

                latestChats.forEach { chat ->
                    val chatId = when {
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

                // Apply state updates on the main thread to avoid snapshot violations
                withContext(Dispatchers.Main) {
                    latestChatCache.clear()
                    latestChatCache.putAll(chatCache)

                    pairedPeers.clear()
                    pairedPeers.addAll(newPairedPeers)

                    unpairedPeers.clear()
                    unpairedPeers.addAll(newUnpairedPeers)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pairedPeers.clear()
                    unpairedPeers.clear()
                    latestChatCache.clear()
                }
            }
        }
    }

    fun getLatestChat(chatId: String): DChat? {
        return latestChatCache[chatId]
    }

    fun updateDiscoverable(context: Context, discoverable: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            NearbyDiscoverablePreference.putAsync(context, discoverable)
        }
    }

    fun removePeer(context: Context, peerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Delete all chat messages and associated files for this peer
                ChatHelper.deleteAllChatsByPeerAsync(context, peerId)
                
                // Delete the peer record
                AppDatabase.instance.peerDao().delete(peerId)
                
                // Reload key cache and peers list
                ChatApiManager.loadKeyCacheAsync()
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
            currentMap[peerId] = Clock.System.now()
            onlineMap.value = currentMap
        }
    }

    fun isPeerOnline(peerId: String): Boolean {
        val lastActive = onlineMap.value[peerId] ?: return false
        val now = Clock.System.now()
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

                    if (peer.ip != device.ip) {
                        peer.ip = device.ip
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
                        peer.updatedAt = Clock.System.now()
                        AppDatabase.instance.peerDao().update(peer)
                        loadPeers()
                    }

                    // Update last active time for this peer
                    updatePeerLastActive(device.id)
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
} 