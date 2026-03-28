package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.events.HttpApiEvents
import com.ismartcoding.plain.events.NearbyDeviceFoundEvent
import com.ismartcoding.plain.events.PairingSuccessEvent
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference
import com.ismartcoding.plain.TempData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class PeerViewModel : ViewModel() {
    val pairedPeers = mutableStateListOf<DPeer>()
    val unpairedPeers = mutableStateListOf<DPeer>()
    internal val latestChatCacheInternal = mutableMapOf<String, DChat>()
    val onlineMap = mutableStateOf<Map<String, kotlin.time.Instant>>(emptyMap())
    private var eventJob: Job? = null

    init { startEventListening() }

    private fun startEventListening() {
        eventJob = viewModelScope.launch {
            Channel.sharedFlow.collect { event ->
                when (event) {
                    is HttpApiEvents.MessageCreatedEvent -> viewModelScope.launch { loadPeers() }
                    is NearbyDeviceFoundEvent -> handleDeviceFoundInternal(event)
                    is PairingSuccessEvent -> updatePeerLastActive(event.deviceId)
                }
            }
        }
    }

    override fun onCleared() { super.onCleared(); eventJob?.cancel() }

    fun loadPeers() = loadPeersInternal()

    fun getLatestChat(chatId: String): DChat? = latestChatCacheInternal[chatId]

    fun updateDiscoverable(context: Context, discoverable: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            NearbyDiscoverablePreference.putAsync(context, discoverable)
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
                    if (peer != null) { peer.key = ""; peer.status = "channel"; peerDao.update(peer) }
                } else {
                    peerDao.delete(peerId)
                }
                ChatCacheManager.loadKeyCacheAsync()
                loadPeers()
            } catch (_: Exception) {}
        }
    }

    fun updatePeerLastActive(peerId: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val currentMap = onlineMap.value.toMutableMap()
            currentMap[peerId] = TimeHelper.now()
            onlineMap.value = currentMap
        }
    }

    fun isPeerOnline(peerId: String): Boolean {
        val lastActive = onlineMap.value[peerId] ?: return false
        return (TimeHelper.now() - lastActive) <= 15.seconds
    }

    fun getPeerOnlineStatus(peerId: String): Boolean? {
        return if (onlineMap.value.containsKey(peerId)) isPeerOnline(peerId) else false
    }
}
