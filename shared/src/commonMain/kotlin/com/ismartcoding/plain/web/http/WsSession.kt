package com.ismartcoding.plain.web.http

/**
 * Platform-agnostic WebSocket session exposed to commonMain route handlers.
 *
 * The platform layer (Ktor's `DefaultWebSocketServerSession`, SwiftNIO's
 * `NIOWebSocketServerUpgrader`) implements this so business code can read
 * incoming binary frames and send binary/text frames without depending on
 * a specific WebSocket library.
 */
interface WsSession {
    /** Suspend until the next binary frame arrives, or return `null` when the
     *  underlying connection is closed. */
    suspend fun receiveBinary(): ByteArray?

    /** Suspend until the next text frame arrives, or return `null` when the
     *  underlying connection is closed. */
    suspend fun receiveText(): String?

    /** Send a binary frame to the client. */
    suspend fun sendBinary(bytes: ByteArray)

    /** Send a text frame to the client. */
    suspend fun sendText(text: String)

    /** Close the session with the given close code and reason. */
    suspend fun close(code: Int, reason: String)

    /** Close the session without a specific reason. */
    suspend fun close()

    /** Remote host address of the connected client. */
    val remoteHost: String
}

/**
 * WebSocket route entry collected by [HttpRouter.addWebSocket] and dispatched
 * by the platform layer's WebSocket router.
 */
data class WebSocketRouteEntry(
    val path: String,
    val handler: suspend (WsSession, HttpCall) -> Unit,
)
