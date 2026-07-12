package com.ismartcoding.plain.chat.peer
import com.ismartcoding.plain.platform.subscribeAwareForPeer
import com.ismartcoding.plain.platform.startAwareIfNeeded

import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.platform.chaCha20Encrypt
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.platform.isWifiAwareSupported
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.getStatusWsUrl
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.PeerStatusData
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.SignatureHelper
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.platform.createPeerStatusHttpClient
import com.ismartcoding.plain.platform.discoverPeerDevice
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.min

object PeerStatusManager {
    private const val INITIAL_RECONNECT_DELAY_MS = 1000L
    private const val MAX_RECONNECT_DELAY_MS = 60000L

    private data class PeerState(
        val socketJob: Job? = null,
        val reconnectJob: Job? = null,
        val reconnectAttempts: Int = 0,
        val online: Boolean = false,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val client: HttpClient by lazy { createPeerStatusHttpClient() }
    private val statesFlow = MutableStateFlow<Map<String, PeerState>>(emptyMap())

    private val startedFlow = MutableStateFlow(false)

    fun start() {
        if (startedFlow.value) return
        startedFlow.value = true
        LogCat.d("peer status: start")
        scope.launch { reconnectAll() }
        if (isWifiAwareSupported) {
            ensureAwareStarted()
        }
    }

    fun ensureAwareStarted() {
        if (startAwareIfNeeded()) {
            scope.launch { notifyAwareOfPairedPeers() }
        }
    }

    private suspend fun notifyAwareOfPairedPeers() {
        val peers = AppDatabase.instance.peerDao().getAllPaired()
        LogCat.d("peer status: feeding ${peers.size} paired peers to Aware")
        peers.forEach { subscribeAwareForPeer(it) }
    }

    fun stop() {
        LogCat.d("peer status: stop")
        startedFlow.value = false
        statesFlow.value.keys.toList().forEach { peerId ->
            val state = state(peerId)
            state.reconnectJob?.cancel()
            state.socketJob?.cancel()
            updateState(peerId) { it.copy(reconnectJob = null, socketJob = null, reconnectAttempts = 0) }
            setOnline(peerId, false)
        }
    }

    fun disconnected(peerId: String) {
        LogCat.d("peer status: incoming disconnected peer=$peerId")
        setOnline(peerId, false)
    }

    fun isOnline(peerId: String): Boolean = statesFlow.value[peerId]?.online == true

    fun onlinePeers(): Set<String> = statesFlow.value.entries.filter { it.value.online }.map { it.key }.toSet()

    fun reconnectNow(reason: String) {
        scope.launch {
            LogCat.d("peer status: reconnect triggered reason=$reason")
            reconnectAll()
        }
    }

    private suspend fun reconnectAll() = withIO {
        if (!startedFlow.value) return@withIO
        loadConnectablePeers().forEach { forceReconnectPeer(it, reason = "reconnect_all") }
    }

    private suspend fun reconnectPeer(peerId: String, reason: String) = withIO {
        if (!startedFlow.value) return@withIO
        updateState(peerId) { it.copy(reconnectJob = null) }
        val state = state(peerId)
        if (state.socketJob != null) {
            LogCat.d("peer status: reconnect skipped peer=$peerId reason=$reason active_socket=true")
            return@withIO
        }
        val peer = AppDatabase.instance.peerDao().getById(peerId) ?: return@withIO
        if (!shouldConnect(peer)) return@withIO
        val key = PeerCacher.getKeyBytes(peer.id) ?: return@withIO

        LogCat.d("peer status: reconnect peer=$peerId reason=$reason")
        discoverPeerDevice(peer.id, key)
        delay(500)

        val refreshedPeer = AppDatabase.instance.peerDao().getById(peer.id) ?: peer
        if (refreshedPeer.ip.isEmpty() || refreshedPeer.port <= 0) {
            scheduleReconnect(peer.id)
            return@withIO
        }
        openSocket(refreshedPeer, key)
    }

    private suspend fun forceReconnectPeer(peer: DPeer, reason: String) = withIO {
        if (!startedFlow.value) return@withIO
        val state = state(peer.id)
        state.reconnectJob?.cancel()
        updateState(peer.id) { it.copy(reconnectJob = null) }
        if (peer.ip.isEmpty()) {
            return@withIO
        }
        if (state.socketJob != null) {
            LogCat.d("peer status: reconnect skipped peer=${peer.id} reason=$reason active_socket=true")
            return@withIO
        }
        val key = PeerCacher.getKeyBytes(peer.id) ?: return@withIO
        LogCat.d("peer status: reconnect peer=${peer.id} reason=$reason")
        openSocket(peer, key)
    }

    private suspend fun openSocket(peer: DPeer, key: ByteArray) = withIO {
        val peerId = peer.id
        val wsUrl = peer.getStatusWsUrl()
        LogCat.d("peer status: open socket peer=$peerId url=$wsUrl")

        val timestamp = TimeHelper.nowMillis().toString()
        val signature = SignatureHelper.signTextAsync("$timestamp${TempData.clientId}")
        val payload = chaCha20Encrypt(key, "$signature|$timestamp|${TempData.clientId}")

        val job = scope.launch {
            val currentJob = coroutineContext[Job]!!
            try {
                client.webSocket("$wsUrl?cid=${TempData.clientId}") {
                    send(Frame.Binary(true, payload))
                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        val text = frame.readText()
                        if (text != "ok") continue
                        updateState(peerId) {
                            it.copy(reconnectJob = null, reconnectAttempts = 0)
                        }
                        setOnline(peerId, true)
                        LogCat.d("peer status: outgoing online peer=$peerId")
                    }
                }
            } catch (e: Exception) {
                LogCat.e("peer status: outgoing failed peer=$peerId message=${e.message}")
            } finally {
                handleClosed(peerId, currentJob)
            }
        }

        val state = state(peerId)
        if (state.reconnectJob == null) {
            updateState(peerId) { it.copy(socketJob = job) }
        } else {
            job.cancel()
        }
    }

    private fun handleClosed(peerId: String, job: Job) {
        val s = state(peerId)
        val tracked = s.socketJob == null || s.socketJob == job
        if (!tracked) {
            LogCat.d("peer status: close ignored peer=$peerId tracked=false")
            return
        }
        updateState(peerId) { it.copy(socketJob = null) }
        setOnline(peerId, false)
        scheduleReconnect(peerId)
    }

    private fun scheduleReconnect(peerId: String) {
        if (!startedFlow.value) {
            LogCat.d("peer status: schedule reconnect skipped peer=$peerId stopped=true")
            return
        }
        val state = state(peerId)
        if (state.socketJob != null) {
            LogCat.d("peer status: schedule reconnect skipped peer=$peerId active_socket=true")
            return
        }
        if (state.reconnectJob != null) {
            LogCat.d("peer status: schedule reconnect skipped peer=$peerId pending_backoff=true")
            return
        }
        val newAttempts = state.reconnectAttempts + 1
        val delayMs = min(MAX_RECONNECT_DELAY_MS, INITIAL_RECONNECT_DELAY_MS * (1L shl min(newAttempts - 1, 6)))
        LogCat.d("peer status: schedule reconnect peer=$peerId attempt=$newAttempts delay=${delayMs}ms")
        val reconnectJob = scope.launch {
            delay(delayMs)
            reconnectPeer(peerId, reason = "backoff")
        }
        updateState(peerId) { it.copy(reconnectJob = reconnectJob, reconnectAttempts = newAttempts) }
    }

    private suspend fun loadConnectablePeers(): List<DPeer> = withIO {
        val peers = AppDatabase.instance.peerDao().getAllPaired()
        val connectable = peers.filter { shouldConnect(it) }
        LogCat.d("peer status: peers total=${peers.size} connectable=${connectable.size}")
        connectable
    }

    private fun shouldConnect(peer: DPeer): Boolean {
        return peer.status == "paired" && peer.key.isNotEmpty() && TempData.clientId < peer.id
    }

    fun setOnline(peerId: String, online: Boolean) {
        val current = state(peerId)
        if (current.online == online) return
        updateState(peerId) { it.copy(online = online) }
        LogCat.d("peer status: online peer=$peerId value=$online")
        PeerManager.setOnlineStatus(peerId, online)
        sendEvent(WebSocketEvent(EventType.PEER_STATUS_UPDATED, JsonHelper.jsonEncode(PeerStatusData(peerId, online))))
    }

    private fun state(peerId: String): PeerState = statesFlow.value[peerId] ?: PeerState()

    private fun updateState(peerId: String, update: (PeerState) -> PeerState) {
        statesFlow.update { map ->
            val current = map[peerId] ?: PeerState()
            map + (peerId to update(current))
        }
    }
}
