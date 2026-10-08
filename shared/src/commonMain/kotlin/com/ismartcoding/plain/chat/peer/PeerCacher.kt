package com.ismartcoding.plain.chat.peer

import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.chat.ChatCacher
import com.ismartcoding.plain.chat.peer.transport.PeerTransportType
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.enums.PeerStatus
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
import kotlin.time.Instant

object PeerCacher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val peersMap = MutableStateFlow<Map<String, DPeer>>(emptyMap())
    val onlineMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    // Transport currently being attempted for a peer, or absent when idle. Set
    // by PeerTransportHost right before each transport attempt and
    // cleared in finally. Observed by ChatPage to show the transport badge
    // only while a send is in flight.
    val currentTransportMap = MutableStateFlow<Map<String, PeerTransportType>>(emptyMap())

    // In-memory Aware running flag per peer. Set from two sources:
    //  1. BLE scan response serviceData (byte[0]) — cheap hint, no GATT needed
    //     (used by PeerTransportPrewarmer.refreshAwareFlagFromScan)
    //  2. GATT DISCOVER reply (NearbyDeviceInfo.awareRunning) — authoritative
    //     (used by PairingTransport.scanAndDiscover, overwrites the scan hint)
    // When false, WifiAwareTransport skips itself immediately instead of
    // waiting 10s+ for buildLink to time out.
    private val awareRunningMap = mutableStateMapOf<String, Boolean>()
    // In-memory Aware supported flag per peer. Same two sources as above.
    // This is the source of truth for whether the peer CURRENTLY supports
    // Aware — formerly mirrored on the DPeer row, now in-memory only since
    // the aware_supported column has been removed.
    private val awareSupportedMap = mutableStateMapOf<String, Boolean>()

    val pairedPeers: StateFlow<List<DPeer>> = combine(peersMap, ChatCacher.latestChatMap, onlineMap) { p, c, o ->
        sortPeers(p.values.filter { it.isPaired() }, c, o)
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val unpairedPeers: StateFlow<List<DPeer>> = combine(peersMap, ChatCacher.latestChatMap, onlineMap) { p, c, o ->
        sortPeers(p.values.filter { it.status == PeerStatus.UNPAIRED }, c, o)
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

    fun setCurrentTransport(peerId: String, transportType: PeerTransportType?) {
        val current = currentTransportMap.value
        if (transportType == null) {
            if (current.containsKey(peerId)) {
                currentTransportMap.value = current - peerId
            }
        } else if (current[peerId] != transportType) {
            currentTransportMap.value = current + (peerId to transportType)
        }
    }

    fun getPeer(peerId: String): DPeer? = peersMap.value[peerId]

    /** Returns whether the peer's Wi-Fi Aware service is currently running (from DISCOVER reply). */
    fun isAwareRunning(peerId: String): Boolean = awareRunningMap[peerId] == true

    /** Stores the peer's Aware running flag in memory, refreshed from the GATT DISCOVER reply. */
    fun setAwareRunning(peerId: String, running: Boolean) {
        awareRunningMap[peerId] = running
    }

    /** Returns whether the peer currently supports Wi-Fi Aware (from DISCOVER reply). */
    fun isAwareSupported(peerId: String): Boolean = awareSupportedMap[peerId] == true

    /** Stores the peer's Aware supported flag in memory, refreshed from the GATT DISCOVER reply. */
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
        if (currentTransportMap.value.containsKey(peerId)) {
            currentTransportMap.value = currentTransportMap.value - peerId
        }
        awareRunningMap.remove(peerId)
        awareSupportedMap.remove(peerId)
    }

    suspend fun load() = withIO {
        val peers = RustPeerStore.getAll()
        val runtimeMap = peers.associate { peer ->
            peer.id to peer
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
