package com.ismartcoding.plain.chat.peer.transport

import com.ismartcoding.plain.chat.peer.RustPeerStore
import com.ismartcoding.plain.platform.PeerByteSocket
import com.ismartcoding.plain.platform.PlatformLock
import com.ismartcoding.plain.platform.openAwareByteSocket
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64

object PeerSocketHost {
    private class Slot { var socket: PeerByteSocket? = null; var closed = false }
    private val lock = PlatformLock()
    private val slots = mutableMapOf<String, Slot>()

    suspend fun handle(method: String, params: JsonObject): JsonElement {
        if (method == "peerTransportSocketCloseAll") { disconnect(); return JsonPrimitive(true) }
        val token = params.getValue("token").jsonPrimitive.content
        if (method == "peerTransportSocketOpen") {
            val slot = Slot()
            lock.withLock { check(slots.size < 16 && !slots.containsKey(token)) { "Socket handle unavailable" }; slots[token] = slot }
            try {
                val socket = openAwareByteSocket(RustPeerStore.decode(params.getValue("peer")))
                lock.withLock {
                    if (slot.closed) { socket.close(); error("Socket handle canceled") }
                    slot.socket = socket
                }
                return JsonPrimitive(true)
            } catch (error: Throwable) { close(token); throw error }
        }
        if (method == "peerTransportSocketClose") { close(token); return JsonPrimitive(true) }
        val socket = lock.withLock { checkNotNull(slots[token]?.takeUnless { it.closed }?.socket) { "Socket handle unavailable" } }
        return when (method) {
            "peerTransportSocketRead" -> {
                val length = params.getValue("length").jsonPrimitive.int
                require(length in 1..32768)
                val bytes = socket.read(length)
                buildJsonObject {
                    put("eof", bytes != null && bytes.isEmpty())
                    put("bytes", bytes?.let { JsonPrimitive(Base64.encode(it)) } ?: JsonNull)
                }
            }
            "peerTransportSocketWrite" -> {
                val bytes = Base64.decode(params.getValue("bytes").jsonPrimitive.content)
                require(bytes.size <= 32768)
                socket.write(bytes)
                JsonPrimitive(bytes.size)
            }
            else -> error("Unknown socket operation")
        }
    }
    private fun close(token: String) {
        val slot = lock.withLock { slots[token]?.also { it.closed = true } } ?: return
        slot.socket?.close()
        lock.withLock { if (slots[token] === slot) slots.remove(token) }
    }
    fun disconnect() {
        val tokens = lock.withLock { slots.keys.toList() }
        var failure: Throwable? = null
        tokens.forEach { token ->
            try { close(token) } catch (error: Throwable) { failure = error }
        }
        failure?.let { throw it }
    }
}
