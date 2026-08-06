package com.ismartcoding.plain.platform

import com.ismartcoding.plain.httpserver.http.WsSession
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlin.concurrent.Volatile

/**
 * Kotlin-side WebSocket session backed by a SwiftNIO WebSocket channel.
 *
 * Swift creates the session with an [IosWsTransport] (a thin protocol that
 * forwards `sendBinary`/`sendText`/`close` back into the SwiftNIO channel)
 * and then drives it by calling [onBinaryFrame], [onTextFrame], and
 * [onClose] as frames arrive from the network.
 *
 * The commonMain WebSocket route handler (see
 * [com.ismartcoding.plain.httpserver.routes.addWebSocketRoutes]) calls
 * [receiveBinary] in a loop — that call suspends on [frameChannel] until
 * Swift delivers the next frame or closes the connection.
 *
 * The [remoteHost] is captured from the upgraded HTTP request so route
 * handlers can attribute login attempts and rate-limit by IP.
 */
class NioWsSession(
    override val remoteHost: String,
    private val transport: IosWsTransport,
) : WsSession {

    /** Inbound frame channel; fed by Swift via [onBinaryFrame] / [onTextFrame]. */
    private val frameChannel = Channel<ByteArray>(Channel.UNLIMITED)

    @Volatile
    private var closed = false

    // --- Swift → Kotlin: frame delivery ----------------------------------

    /**
     * Called by Swift when a binary WebSocket frame arrives. The bytes are
     * enqueued on [frameChannel] so the next [receiveBinary] call resumes.
     */
    fun onBinaryFrame(data: ByteArray) {
        if (!closed) {
            frameChannel.trySend(data)
        }
    }

    /**
     * Called by Swift when a text WebSocket frame arrives. The text is
     * UTF-8 encoded and enqueued so [receiveBinary] can surface it (matching
     * Ktor's behavior where text frames are delivered as bytes).
     */
    fun onTextFrame(text: String) {
        if (!closed) {
            frameChannel.trySend(text.encodeToByteArray())
        }
    }

    /**
     * Called by Swift when the WebSocket connection is closed (either by the
     * client or by the server). Closes [frameChannel] so suspended
     * [receiveBinary] / [receiveText] calls resume with `null`.
     */
    fun onClose() {
        if (!closed) {
            closed = true
            frameChannel.close()
        }
    }

    // --- Kotlin → Swift: WsSession implementation ------------------------

    override suspend fun receiveBinary(): ByteArray? {
        if (closed) return null
        val result: ChannelResult<ByteArray> = frameChannel.receiveCatching()
        return result.getOrNull()
    }

    override suspend fun receiveText(): String? {
        if (closed) return null
        val result: ChannelResult<ByteArray> = frameChannel.receiveCatching()
        return result.getOrNull()?.decodeToString()
    }

    override suspend fun sendBinary(bytes: ByteArray) {
        if (!closed) {
            transport.sendBinary(bytes)
        }
    }

    override suspend fun sendText(text: String) {
        if (!closed) {
            transport.sendText(text)
        }
    }

    override suspend fun close(code: Int, reason: String) {
        if (!closed) {
            closed = true
            frameChannel.close()
            transport.close(code, reason)
        }
    }

    override suspend fun close() {
        close(1000, "")
    }
}
