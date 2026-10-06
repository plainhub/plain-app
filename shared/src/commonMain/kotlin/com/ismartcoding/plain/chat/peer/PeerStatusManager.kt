package com.ismartcoding.plain.chat.peer

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.PeerStatusData
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.sendEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.*
import kotlin.concurrent.Volatile

object PeerStatusManager {
    @Volatile private var online = emptySet<String>()
    private val commands = Channel<String>(64)
    init {
        coIO {
            for (action in commands) {
                try {
                    RustContentApi.postJsonOrThrow("chat/peer-status", buildJsonObject { put("action", action) })
                    PeerStatusProjection.refresh()
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { LogCat.e("Rust peer status control", error) }
            }
        }
    }
    private fun request(action: String) {
        if (!commands.trySend(action).isSuccess) LogCat.e("Peer status control queue is full")
    }
    fun start() { request("start"); ensureAwareStarted() }
    fun stop() = request("stop")
    fun reconnectNow(reason: String) { LogCat.d("Peer status reconnect: $reason"); request("reconnect") }
    fun ensureAwareStarted() = request("ensureAware")
    fun isOnline(peerId: String): Boolean = peerId in online
    fun onlinePeers(): Set<String> = online
    internal fun applySnapshot(current: Set<String>) {
        val previous = online
        online = current
        (previous + current).forEach { id ->
            val active = id in current
            if ((id in previous) != active) {
                PeerManager.setOnlineStatus(id, active)
                sendEvent(WebSocketEvent(EventType.PEER_STATUS_UPDATED, JsonHelper.jsonEncode(PeerStatusData(id, active))))
            }
        }
    }
}
