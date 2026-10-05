package com.ismartcoding.plain.chat.peer.transport

import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.db.DPeer
import io.ktor.utils.io.ByteReadChannel

/** The three transports a peer message/file can be routed over. */
enum class PeerTransportType { LAN, AWARE, BLE }

interface PeerTransport {
    val type: PeerTransportType
    suspend fun send(peer: DPeer, request: SignedRequest, keyBytes: ByteArray): GraphQLResponse
    suspend fun downloadFile(peer: DPeer, fileId: String): DownloadedResponse
}

class TransportUnavailable(
    transportType: PeerTransportType,
    peerId: String,
    cause: Throwable? = null,
) : Exception("transport=${transportType.name.lowercase()} peer=$peerId unavailable", cause)

/**
 * Transport-agnostic download result. [status] is the HTTP status code of the
 * underlying request; [channel] is a [ByteReadChannel] that streams the file
 * bytes regardless of which transport produced them (LAN/Aware stream the live
 * HTTP body, BLE streams chunked RPC responses through a pipelined channel).
 *
 * [onClose] is invoked when the caller releases the response, so transports
 * that run a background download coroutine (BLE) can cancel it and tear down
 * their connection.
 */
class DownloadedResponse(
    val status: Int,
    val channel: ByteReadChannel,
    private val onClose: (() -> Unit)? = null,
) : AutoCloseable {
    override fun close() {
        onClose?.invoke()
    }
}
