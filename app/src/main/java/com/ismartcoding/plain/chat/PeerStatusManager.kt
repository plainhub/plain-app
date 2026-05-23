package com.ismartcoding.plain.chat

import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.chat.discover.NearbyDiscoverManager
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.getStatusWsUrl
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.PeerOnlineStatusChangedEvent
import com.ismartcoding.plain.events.PeerStatusData
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.SignatureHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.min

object PeerStatusManager {
    private const val INITIAL_RECONNECT_DELAY_MS = 1000L
    private const val MAX_RECONNECT_DELAY_MS = 60000L

    private data class PeerState(
        var socket: WebSocket? = null,
        var reconnectJob: Job? = null,
        var reconnectAttempts: Int = 0,
        var online: Boolean = false,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client: OkHttpClient by lazy {
        HttpClientManager.createUnsafeOkHttpClient()
            .newBuilder()
            .connectTimeout(500, TimeUnit.MILLISECONDS)
            .pingInterval(15, TimeUnit.SECONDS)
            .build()
    }
    private val states = ConcurrentHashMap<String, PeerState>()

    private var started = false

    @Synchronized
    fun start() {
        if (started) return
        started = true
        LogCat.d("peer status: start")
        scope.launch { reconnectAll() }
    }

    @Synchronized
    fun stop() {
        LogCat.d("peer status: stop")
        started = false
        states.keys.toList().forEach { peerId ->
            val state = state(peerId)
            state.reconnectJob?.cancel()
            state.socket?.cancel()
            state.reconnectJob = null
            state.socket = null
            state.reconnectAttempts = 0
            setOnline(peerId, false)
        }
    }

    @Synchronized
    fun disconnected(peerId: String) {
        LogCat.d("peer status: incoming disconnected peer=$peerId")
        setOnline(peerId, false)
    }

    @Synchronized
    fun isOnline(peerId: String): Boolean = states[peerId]?.online == true

    fun reconnectNow(reason: String) {
        scope.launch {
            LogCat.d("peer status: reconnect triggered reason=$reason")
            reconnectAll()
        }
    }

    private suspend fun reconnectAll() {
        if (!started) return
        loadConnectablePeers().forEach { forceReconnectPeer(it, reason = "reconnect_all") }
    }

    private suspend fun reconnectOfflinePeers(reason: String) {
        if (!started) return
        LogCat.d("peer status: reconnect triggered reason=$reason")
        loadConnectablePeers()
            .filter { !isOnline(it.id) }
            .forEach { forceReconnectPeer(it, reason = reason) }
    }

    private suspend fun reconnectPeer(peerId: String, reason: String) {
        if (!started) return
        val state = state(peerId)
        state.reconnectJob = null
        if (state.socket != null) {
            LogCat.d("peer status: reconnect skipped peer=$peerId reason=$reason active_socket=true")
            return
        }
        val peer = AppDatabase.instance.peerDao().getById(peerId) ?: return
        if (!shouldConnect(peer)) return
        val key = ChatCacheManager.peerKeyCache[peer.id] ?: return

        LogCat.d("peer status: reconnect peer=$peerId reason=$reason")
        NearbyDiscoverManager.discoverSpecificDevice(peer.id, key)
        delay(500)

        val refreshedPeer = AppDatabase.instance.peerDao().getById(peer.id) ?: peer
        if (refreshedPeer.ip.isEmpty() || refreshedPeer.port <= 0) {
            scheduleReconnect(peer.id)
            return
        }
        openSocket(refreshedPeer, key)
    }

    private suspend fun forceReconnectPeer(peer: DPeer, reason: String) {
        if (!started) return
        val state = state(peer.id)
        state.reconnectJob?.cancel()
        state.reconnectJob = null
        if (state.socket != null) {
            LogCat.d("peer status: reconnect skipped peer=$peer.Id reason=$reason active_socket=true")
            return
        }
        val key = ChatCacheManager.peerKeyCache[peer.id] ?: return
        LogCat.d("peer status: reconnect peer=$peer.Id reason=$reason")
        openSocket(peer, key)
    }

    private suspend fun openSocket(peer: DPeer, key: ByteArray) {
        val peerId = peer.id
        LogCat.d("peer status: open socket peer=$peerId url=${peer.getStatusWsUrl()}")

        val timestamp = System.currentTimeMillis().toString()
        val signature = SignatureHelper.signTextAsync("$timestamp${TempData.clientId}")
        val payload = CryptoHelper.chaCha20Encrypt(key, "$signature|$timestamp|${TempData.clientId}")
        val request = Request.Builder()
            .url("${peer.getStatusWsUrl()}?cid=${TempData.clientId}")
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(payload.toByteString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text != "ok") return
                val s = state(peerId)
                s.reconnectJob?.cancel()
                s.reconnectJob = null
                s.reconnectAttempts = 0
                s.socket = webSocket
                setOnline(peerId, true)
                LogCat.d("peer status: outgoing online peer=$peerId")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                handleClosed(webSocket)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                LogCat.e("peer status: outgoing failed peer=$peerId code=${response?.code} message=${t.message}")
                handleClosed(webSocket)
            }

            private fun handleClosed(webSocket: WebSocket) {
                val s = state(peerId)
                val tracked = s.socket == null || s.socket == webSocket
                if (!tracked) {
                    LogCat.d("peer status: close ignored peer=$peerId tracked=false")
                    return
                }
                s.socket = null
                setOnline(peerId, false)
                scheduleReconnect(peerId)
            }
        }

        val socket = client.newWebSocket(request, listener)
        val state = state(peerId)
        if (state.reconnectJob == null) {
            state.socket = socket
        } else {
            socket.cancel()
        }
    }

    private fun disconnectOutgoing(peerId: String, reason: String) {
        val state = state(peerId)
        state.socket?.cancel()
        state.socket = null
        if (state.online) {
            LogCat.d("peer status: outgoing offline peer=$peerId reason=$reason")
        }
        setOnline(peerId, false)
    }

    private fun scheduleReconnect(peerId: String) {
        if (!started) {
            LogCat.d("peer status: schedule reconnect skipped peer=$peerId stopped=true")
            return
        }
        val state = state(peerId)
        if (state.socket != null) {
            LogCat.d("peer status: schedule reconnect skipped peer=$peerId active_socket=true")
            return
        }
        if (state.reconnectJob != null) {
            LogCat.d("peer status: schedule reconnect skipped peer=$peerId pending_backoff=true")
            return
        }
        state.reconnectAttempts++
        val delayMs = min(MAX_RECONNECT_DELAY_MS, INITIAL_RECONNECT_DELAY_MS * (1L shl min(state.reconnectAttempts - 1, 6)))
        LogCat.d("peer status: schedule reconnect peer=$peerId attempt=${state.reconnectAttempts} delay=${delayMs}ms")
        state.reconnectJob = scope.launch {
            delay(delayMs)
            reconnectPeer(peerId, reason = "backoff")
        }
    }

    private suspend fun loadConnectablePeers(): List<DPeer> {
        val peers = AppDatabase.instance.peerDao().getAllPaired()
        val connectable = peers.filter { shouldConnect(it) }
        LogCat.d("peer status: peers total=${peers.size} connectable=${connectable.size}")
        return connectable
    }

    private fun shouldConnect(peer: DPeer): Boolean {
        return peer.status == "paired" && peer.key.isNotEmpty() && TempData.clientId < peer.id
    }

    @Synchronized
    fun setOnline(peerId: String, online: Boolean) {
        val state = state(peerId)
        if (state.online == online) return
        state.online = online
        LogCat.d("peer status: online peer=$peerId value=$online")
        sendEvent(PeerOnlineStatusChangedEvent(peerId, online))
        sendEvent(WebSocketEvent(EventType.PEER_STATUS_UPDATED, JsonHelper.jsonEncode(PeerStatusData(peerId, online))))
    }

    private fun state(peerId: String): PeerState = states.getOrPut(peerId) { PeerState() }
}
