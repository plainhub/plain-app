package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.httpserver.http.WsSession
import com.ismartcoding.plain.platform.PlainWebSocketSession
import kotlinx.serialization.json.*

internal class RustWsSession(private val socket: PlainWebSocketSession) : WsSession {
    override suspend fun receiveBinary(): ByteArray? {
        for (frame in socket.incoming) frame.binary?.let { return it }
        return null
    }
    override suspend fun sendBinary(bytes: ByteArray) = socket.sendBinary(bytes)
    override suspend fun close(code: Int, reason: String) {
        socket.sendText(buildJsonObject { put("kind", "close"); put("code", code); put("reason", reason) }.toString())
    }
    override suspend fun close() = close(1000, "")
}
