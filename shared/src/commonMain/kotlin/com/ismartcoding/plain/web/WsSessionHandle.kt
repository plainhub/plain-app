package com.ismartcoding.plain.web

/**
 * Platform-agnostic handle to a live WebSocket session.
 *
 * Android implements this with Ktor's `DefaultWebSocketServerSession`; iOS will
 * implement it with SwiftNIO. Business logic in commonMain holds instances of
 * this interface and never touches platform-specific WebSocket types directly.
 */
interface WsSessionHandle {
    val id: Long
    val clientId: String

    suspend fun send(bytes: ByteArray)

    suspend fun close(code: Int, reason: String)

    suspend fun close()
}
