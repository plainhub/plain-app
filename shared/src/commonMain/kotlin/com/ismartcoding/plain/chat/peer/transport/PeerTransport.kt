package com.ismartcoding.plain.chat.peer.transport

import com.ismartcoding.plain.api.addClientHeaders
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.CryptoKeyAttribute
import com.ismartcoding.plain.platform.chaCha20Decrypt
import com.ismartcoding.plain.platform.chaCha20Encrypt
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
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
    client: HttpClient,
    url: String,
    body: String,
    channelId: String,
): GraphQLResponse = withContext(Dispatchers.Default) {
    val tid = transportType.name.lowercase()
    val cryptoKey = runCatching {
        client.attributes.getOrNull(CryptoKeyAttribute)
    }.getOrNull()
    val response = try {
        client.post(url) {
            if (cryptoKey != null) {
                val encrypted = chaCha20Encrypt(cryptoKey, body)
                setBody(encrypted)
                contentType(ContentType.Application.OctetStream)
            } else {
                setBody(body)
                contentType(ContentType.Application.Json)
            }
            addClientHeaders()
            if (channelId.isNotEmpty()) {
                header("c-cid", channelId)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LogCat.d("$tid request to peer $peerId threw ${e::class.simpleName}: ${e.message}")
        throw TransportUnavailable(transportType, peerId, e)
    }
    val responseBody = if (cryptoKey != null) {
        val encryptedBytes = response.readBytes()
        val decrypted = chaCha20Decrypt(cryptoKey, encryptedBytes)
        decrypted?.decodeToString() ?: encryptedBytes.decodeToString()
    } else {
        response.bodyAsText()
    }
    if (!response.status.isSuccess()) {
        LogCat.e("$tid GraphQL request failed: ${response.status.value} body=${responseBody.take(200)}")
        GraphQLResponse(null, null, Exception("${response.status.value} - ${response.status.description}"))
    } else {
        GraphQLResponseParser.parse(responseBody)
    }
}

internal suspend fun executeDownloadRequest(
    transportType: PeerTransportType,
    peerId: String,
    client: HttpClient,
    url: String,
): DownloadedResponse = withContext(Dispatchers.Default) {
    val tid = transportType.name.lowercase()
    val response = try {
        client.get(url) {
            addClientHeaders()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LogCat.d("$tid download to peer $peerId threw ${e::class.simpleName}: ${e.message}")
        throw TransportUnavailable(transportType, peerId, e)
    }
    if (!response.status.isSuccess()) {
        LogCat.e("$tid downloadFile error: ${response.status.value}")
        throw TransportUnavailable(transportType, peerId, null)
    }
    DownloadedResponse(response.status.value, response.bodyAsChannel())
}
