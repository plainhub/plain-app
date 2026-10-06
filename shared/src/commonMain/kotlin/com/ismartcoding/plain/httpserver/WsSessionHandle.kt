package com.ismartcoding.plain.httpserver

/**
 * Platform-agnostic handle to a live WebSocket session.
 *
 * The bridge's [RustWsSession] implements it over the WebSocket it holds to the
 * Rust listener. Business logic in commonMain holds instances of this interface
 * and never touches platform-specific WebSocket types directly.
 */
interface WsSessionHandle {
    val id: Long
    val clientId: String

    suspend fun send(bytes: ByteArray)

    suspend fun close(code: Int, reason: String)

    suspend fun close()
}
