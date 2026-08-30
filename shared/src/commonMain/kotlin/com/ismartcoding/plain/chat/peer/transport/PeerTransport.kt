package com.ismartcoding.plain.chat.peer.transport

import com.ismartcoding.plain.api.clientHeadersWith
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.PlainHttpClient
import com.ismartcoding.plain.platform.get
import com.ismartcoding.plain.platform.post
import com.ismartcoding.plain.platform.chaCha20Decrypt
import com.ismartcoding.plain.platform.chaCha20Encrypt
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

internal suspend fun executeGraphQLRequest(
    transportType: PeerTransportType,
    peerId: String,
    client: PlainHttpClient,
    url: String,
    body: String,
    channelId: String,
): GraphQLResponse = withContext(Dispatchers.Default) {
    val tid = transportType.name.lowercase()
    val cryptoKey = client.cryptoKey
    val response = try {
        if (cryptoKey != null) {
            client.post(
                url,
                body = chaCha20Encrypt(cryptoKey, body),
                contentType = "application/octet-stream",
                headers = clientHeadersWith("c-cid" to channelId),
            )
        } else {
            client.post(
                url,
                body = body.encodeToByteArray(),
                contentType = "application/json",
                headers = clientHeadersWith("c-cid" to channelId),
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LogCat.d("$tid request to peer $peerId threw ${e::class.simpleName}: ${e.message}")
        throw TransportUnavailable(transportType, peerId, e)
    }
    response.use {
        val responseBody = if (cryptoKey != null) {
            val encryptedBytes = it.bodyAsBytes()
            val decrypted = chaCha20Decrypt(cryptoKey, encryptedBytes)
            decrypted?.decodeToString() ?: encryptedBytes.decodeToString()
        } else {
            it.bodyAsText()
        }
        if (!it.isSuccess()) {
            LogCat.e("$tid GraphQL request failed: ${it.status} body=${responseBody.take(200)}")
            GraphQLResponse(null, null, Exception("${it.status.value} - ${it.status.description}"))
        } else {
            GraphQLResponseParser.parse(responseBody)
        }
    }
}

internal suspend fun executeDownloadRequest(
    transportType: PeerTransportType,
    peerId: String,
    client: PlainHttpClient,
    url: String,
): DownloadedResponse = withContext(Dispatchers.Default) {
    val tid = transportType.name.lowercase()
    val response = try {
        client.get(url, headers = clientHeadersWith())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LogCat.d("$tid download to peer $peerId threw ${e::class.simpleName}: ${e.message}")
        throw TransportUnavailable(transportType, peerId, e)
    }
    if (!response.isSuccess()) {
        LogCat.e("$tid downloadFile error: ${response.status}")
        response.close()
        throw TransportUnavailable(transportType, peerId, null)
    }
    DownloadedResponse(response.status.value, response.channel) { response.close() }
}
