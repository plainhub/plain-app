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

    // In-memory Aware running flag per peer. Set from two sources:
    //  1. BLE scan response serviceData (byte[0]) — cheap hint, no GATT needed
    //     (used by PeerTransportPrewarmer.refreshAwareFlagFromScan)
    //  2. GATT DISCOVER reply (DDiscoverReply.awareRunning) — authoritative
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

    /**
     * 在 [peerId] 对应 peer 的副本上执行 [block] 进行修改，
     * 然后将修改后的副本写回缓存和数据库。
     *
     * 使用副本确保缓存内的旧对象不被破坏，从而让 [pairedPeers]/[unpairedPeers]
     * 等 StateFlow 能通过引用差异检测到变化并发射更新（StateFlow 的
     * distinctUntilChanged 基于内容比较，原地修改缓存引用会导致新旧 list
     * 内容相同而不发射）。
     *
     * 内部已通过 [withIO] 切换到 IO dispatcher，调用方无需自行指定。
     *
     * @return 修改后的新 peer 实例；若 peerId 不存在则返回 null。
     */
    suspend fun mutatePeer(peerId: String, block: (DPeer) -> Unit): DPeer? = withIO {
        val current = peersMap.value
        val runtime = current[peerId] ?: return@withIO null
        val newPeer = runtime.peer.copy()
        block(newPeer)
        AppDatabase.instance.peerDao().update(newPeer)
        peersMap.value = current + (peerId to runtime.copy(peer = newPeer))
        newPeer
    }

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
        awareRunningMap.remove(peerId)
        awareSupportedMap.remove(peerId)
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
