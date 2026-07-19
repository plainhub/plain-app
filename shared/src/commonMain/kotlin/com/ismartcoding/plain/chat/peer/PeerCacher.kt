package com.ismartcoding.plain.chat.peer

import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.chat.ChatCacher
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DPeer
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Instant

object PeerCacher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val peersMap = MutableStateFlow<Map<String, PeerRuntime>>(emptyMap())
    val onlineMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    // In-memory Aware running flag per peer, refreshed from BLE scan response
    // serviceData (FLAG_AWARE_RUNNING). When false, WifiAwareTransport skips
    // itself immediately instead of waiting 10s+ for buildLink to time out.
    private val awareRunningMap = mutableStateMapOf<String, Boolean>()
    // In-memory Aware supported flag per peer, refreshed from BLE scan response
    // serviceData (FLAG_AWARE_SUPPORTED). This is the source of truth for whether
    // the peer CURRENTLY supports Aware — formerly mirrored on the DPeer row,
    // now BLE-scan-only since peers are identified by clientId (not BLE MAC)
    // and the aware_supported column has been removed.
    private val awareSupportedMap = mutableStateMapOf<String, Boolean>()

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

    /** Returns whether the peer's Wi-Fi Aware service is currently running (from BLE scan response). */
    fun isAwareRunning(peerId: String): Boolean = awareRunningMap[peerId] == true

    /** Stores the peer's Aware running flag in memory, refreshed from BLE scan response serviceData. */
    fun setAwareRunning(peerId: String, running: Boolean) {
        awareRunningMap[peerId] = running
    }

    /** Returns whether the peer currently supports Wi-Fi Aware (from BLE scan response serviceData). */
    fun isAwareSupported(peerId: String): Boolean = awareSupportedMap[peerId] == true

    /** Stores the peer's Aware supported flag in memory, refreshed from BLE scan response serviceData. */
    fun setAwareSupported(peerId: String, supported: Boolean) {
        awareSupportedMap[peerId] = supported
    }

    fun removePeer(peerId: String) {
        val currentPeers = peersMap.value
        val currentOnline = onlineMap.value
        val newPeers = if (currentPeers.containsKey(peerId)) currentPeers - peerId else currentPeers
        val newOnline = if (currentOnline.containsKey(peerId)) currentOnline - peerId else currentOnline
        if (newPeers !== currentPeers) peersMap.value = newPeers
        if (newOnline !== currentOnline) onlineMap.value = newOnline
        awareRunningMap.remove(peerId)
        awareSupportedMap.remove(peerId)
    }

    fun updatePeer(peer: DPeer) {
        val current = peersMap.value
        val runtime = current[peer.id] ?: return
        if (runtime.peer === peer) return
        peersMap.value = current + (peer.id to runtime.copy(peer = peer))
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun load() = withIO {
        val peers = AppDatabase.instance.peerDao().getAll()
        val runtimeMap = peers.associate { peer ->
            val keyBytes = if (peer.key.isNotEmpty()) Base64Lenient.decode(peer.key) else ByteArray(0)
            val publicKeyBytes = if (peer.publicKey.isNotEmpty()) Base64Lenient.decode(peer.publicKey) else ByteArray(0)
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
