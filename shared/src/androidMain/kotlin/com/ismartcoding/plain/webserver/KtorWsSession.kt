package com.ismartcoding.plain.webserver

import com.ismartcoding.plain.web.http.WsSession
import io.ktor.server.plugins.origin
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import io.ktor.websocket.send

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
        session.close(io.ktor.websocket.CloseReason(code.toShort(), reason))
    }

    override suspend fun close() {
        session.close()
    }
}
