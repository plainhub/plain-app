package com.ismartcoding.plain.chat.peer.transport

import com.ismartcoding.plain.api.addClientHeaders
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.lib.logcat.LogCat
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PeerTransport {
    val id: String
    suspend fun send(peer: DPeer, request: SignedRequest, keyBytes: ByteArray): GraphQLResponse
    suspend fun downloadFile(peer: DPeer, fileId: String): DownloadedResponse
}

class TransportUnavailable(
    transportId: String,
    peerId: String,
    cause: Throwable? = null,
) : Exception("transport=$transportId peer=$peerId unavailable", cause)

class DownloadedResponse(
    val response: HttpResponse,
) : AutoCloseable {
    override fun close() {
    }
}

internal suspend fun executeGraphQLRequest(
    transportId: String,
    peerId: String,
    client: HttpClient,
    url: String,
    body: String,
    channelId: String,
): GraphQLResponse = withContext(Dispatchers.Default) {
    val response = try {
        client.post(url) {
            setBody(body)
            contentType(ContentType.Application.Json)
            addClientHeaders()
            if (channelId.isNotEmpty()) {
                header("c-cid", channelId)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LogCat.d("$transportId request to peer $peerId threw ${e::class.simpleName}: ${e.message}")
        throw TransportUnavailable(transportId, peerId, e)
    }
    val responseBody = response.bodyAsText()
    if (!response.status.isSuccess()) {
        LogCat.e("$transportId GraphQL request failed: ${response.status.value} body=${responseBody.take(200)}")
        GraphQLResponse(null, null, Exception("${response.status.value} - ${response.status.description}"))
    } else {
        GraphQLResponseParser.parse(responseBody)
    }
}

internal suspend fun executeDownloadRequest(
    transportId: String,
    peerId: String,
    client: HttpClient,
    url: String,
): HttpResponse = withContext(Dispatchers.Default) {
    val response = try {
        client.get(url) {
            addClientHeaders()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LogCat.d("$transportId download to peer $peerId threw ${e::class.simpleName}: ${e.message}")
        throw TransportUnavailable(transportId, peerId, e)
    }
    if (!response.status.isSuccess()) {
        LogCat.e("$transportId downloadFile error: ${response.status.value}")
        throw TransportUnavailable(transportId, peerId, null)
    }
    response
}
