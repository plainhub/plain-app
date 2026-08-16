package com.ismartcoding.plain.httpserver.websocket

import com.ismartcoding.plain.events.WebSocketData
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.chaCha20Encrypt
import com.ismartcoding.plain.httpserver.HttpServerManager
import com.ismartcoding.plain.httpserver.setOnlineClientIds
import com.ismartcoding.plain.httpserver.WsSessionHandle

object WebSocketHelper {
    suspend fun sendEventAsync(event: WebSocketEvent) = withIO {
        HttpServerManager.wsSessions.toList().forEach {
            val data = event.data
            if (data is WebSocketData.Text) {
                val token = HttpServerManager.tokenCache[it.clientId]
                if (token != null) {
                    sendSafe(it, addIntPrefixToByteArray(event.type.value, chaCha20Encrypt(token, data.value)))
                }
            } else if (data is WebSocketData.Binary) {
                sendSafe(it, addIntPrefixToByteArray(event.type.value, data.value))
            }
        }
    }

    /**
     * A session stays in [HttpServerManager.wsSessions] until its receive loop
     * exits, so a client that just disconnected can still receive a broadcast
     * (e.g. 60fps screen-mirror frames). Sending to it throws, which would
     * crash the app as an uncaught coroutine exception — drop the dead session
     * instead and keep broadcasting to the rest.
     */
    private suspend fun sendSafe(
        session: WsSessionHandle,
        bytes: ByteArray,
    ) {
        try {
            session.send(bytes)
        } catch (ex: Exception) {
            HttpServerManager.wsSessions.removeAll { it.id == session.id }
            setOnlineClientIds(HttpServerManager.wsSessions.map { it.clientId }.toSet())
        }
    }
}

fun addIntPrefixToByteArray(value: Int, byteArray: ByteArray): ByteArray {
    val intBytes = ByteArray(4) // Int is 4 bytes long
    intBytes[0] = (value shr 24).toByte()
    intBytes[1] = (value shr 16).toByte()
    intBytes[2] = (value shr 8).toByte()
    intBytes[3] = value.toByte()

    return intBytes + byteArray
}
