package com.ismartcoding.plain.chat.peer

import android.util.Base64
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.chat.ChatCacher
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DPeer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Instant

object PeerCacher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val peersMap = MutableStateFlow<Map<String, PeerRuntime>>(emptyMap())
    val onlineMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    val pairedPeers: StateFlow<List<DPeer>> = combine(peersMap, ChatCacher.latestChatMap, onlineMap) { p, c, o ->
        sortPeers(p.values.filter { it.peer.isPaired() }.map { it.peer }, c, o)
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val unpairedPeers: StateFlow<List<DPeer>> = combine(peersMap, ChatCacher.latestChatMap, onlineMap) { p, c, o ->
        sortPeers(p.values.filter { it.peer.status == "unpaired" }.map { it.peer }, c, o)
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val onlinePeerIds: StateFlow<Set<String>> = onlineMap
        .map { it.filterValues { online -> online }.keys.toSet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    fun setOnlineMap(map: Map<String, Boolean>) {
        onlineMap.value = map
    }

    fun setOnline(peerId: String, online: Boolean) {
        val current = onlineMap.value
        if (current[peerId] == online) return
        onlineMap.value = current.toMutableMap().also { it[peerId] = online }
    }

    fun isPeerOnline(peerId: String): Boolean = onlineMap.value[peerId] == true

    fun getPeerOnlineStatus(peerId: String): Boolean? = onlineMap.value[peerId]

    fun getOnlinePeerIds(): Set<String> = onlineMap.value.filterValues { it }.keys.toSet()

    fun getPeer(peerId: String): DPeer? = peersMap.value[peerId]?.peer

    fun getKeyBytes(peerId: String): ByteArray? = peersMap.value[peerId]?.keyBytes?.takeIf { it.isNotEmpty() }

    fun getPublicKeyBytes(peerId: String): ByteArray? = peersMap.value[peerId]?.publicKeyBytes?.takeIf { it.isNotEmpty() }

    fun removePeer(peerId: String) {
        val currentPeers = peersMap.value
        val currentOnline = onlineMap.value
        val newPeers = if (currentPeers.containsKey(peerId)) currentPeers - peerId else currentPeers
        val newOnline = if (currentOnline.containsKey(peerId)) currentOnline - peerId else currentOnline
        if (newPeers !== currentPeers) peersMap.value = newPeers
        if (newOnline !== currentOnline) onlineMap.value = newOnline
    }

    fun updatePeer(peer: DPeer) {
        val current = peersMap.value
        val runtime = current[peer.id] ?: return
        if (runtime.peer === peer) return
        peersMap.value = current + (peer.id to runtime.copy(peer = peer))
    }

    suspend fun load() = withIO {
        val peers = AppDatabase.instance.peerDao().getAll()
        val runtimeMap = peers.associate { peer ->
            val keyBytes = if (peer.key.isNotEmpty()) Base64.decode(peer.key, Base64.NO_WRAP) else ByteArray(0)
            val publicKeyBytes = if (peer.publicKey.isNotEmpty()) Base64.decode(peer.publicKey, Base64.NO_WRAP) else ByteArray(0)
            peer.id to PeerRuntime(peer, keyBytes, publicKeyBytes)
        }
        peersMap.value = runtimeMap
    }

    private fun sortPeers(
        peers: List<DPeer>,
        chatCache: Map<String, DChat>,
        onlineMap: Map<String, Boolean>,
    ): List<DPeer> {
        return peers.sortedWith(
            compareByDescending<DPeer> { chatCache[it.id]?.createdAt ?: Instant.DISTANT_PAST }
                .thenByDescending { onlineMap[it.id] == true }
                .thenByDescending { it.createdAt }
                .thenBy { it.name.lowercase() },
        )
    }
}
