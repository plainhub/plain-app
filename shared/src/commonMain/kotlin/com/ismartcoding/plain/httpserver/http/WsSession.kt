package com.ismartcoding.plain.httpserver.http

/**
 * Platform-agnostic WebSocket session exposed to commonMain route handlers.
 *
 * RustWsSession adapts binary frames from the Rust listener's loopback
 * connection for the remaining host WebSocket handler.
 */
interface WsSession {
    /** Suspend until the next binary frame arrives, or return `null` when the
     *  underlying connection is closed. */
    suspend fun receiveBinary(): ByteArray?

    /** Send a binary frame to the client. */
    suspend fun sendBinary(bytes: ByteArray)

    /** Close the session with the given close code and reason. */
    suspend fun close(code: Int, reason: String)

    /** Close the session without a specific reason. */
    suspend fun close()
}

/**
 * WebSocket route entry collected by [HttpRouter.webSocket] and dispatched
 * by the platform layer's WebSocket router.
 */
data class WebSocketRouteEntry(
    val path: String,
    val handler: suspend (WsSession, HttpCall) -> Unit,
)
