package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.httpserver.http.WsSession
import com.ismartcoding.plain.lib.ktorserver.core.plugins.origin
import com.ismartcoding.plain.lib.ktorserver.DefaultWebSocketServerSession
import com.ismartcoding.plain.lib.ktorserver.websocket.Frame
import com.ismartcoding.plain.lib.ktorserver.websocket.close
import com.ismartcoding.plain.lib.ktorserver.websocket.readBytes
import com.ismartcoding.plain.lib.ktorserver.websocket.readText
import com.ismartcoding.plain.lib.ktorserver.websocket.send

/**
 * Adapts a Ktor [DefaultWebSocketServerSession] to the commonMain [WsSession]
 * interface so WebSocket route handlers can live entirely in shared code.
 */
class KtorWsSession(
    private val session: DefaultWebSocketServerSession,
) : WsSession {
    override val remoteHost: String
        get() = session.call.request.origin.remoteHost

    override suspend fun receiveBinary(): ByteArray? {
        for (frame in session.incoming) {
            if (frame is Frame.Binary) return frame.readBytes()
            if (frame is Frame.Text) {
                // Some clients (e.g. curl) send text frames; we surface them
                // to receiveBinary as a UTF-8 byte array so the commonMain
                // handler can still attempt to decrypt/parse them.
                return frame.readText().encodeToByteArray()
            }
        }
        return null
    }

    override suspend fun receiveText(): String? {
        for (frame in session.incoming) {
            if (frame is Frame.Text) return frame.readText()
            if (frame is Frame.Binary) return frame.readBytes().decodeToString()
        }
        return null
    }

    override suspend fun sendBinary(bytes: ByteArray) {
        session.send(bytes)
    }

    override suspend fun sendText(text: String) {
        session.send(text)
    }

    override suspend fun close(code: Int, reason: String) {
        session.close(com.ismartcoding.plain.lib.ktorserver.websocket.CloseReason(code.toShort(), reason))
    }

    override suspend fun close() {
        session.close()
    }
}
