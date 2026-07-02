package com.ismartcoding.plain.chat.peer.transport

import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.lib.logcat.LogCat
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

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

class DownloadedResponse internal constructor(
    val client: OkHttpClient,
    val response: Response,
) : AutoCloseable {
    override fun close() {
        response.close()
    }
}

internal suspend fun executeGraphQLRequest(
    transportId: String,
    peerId: String,
    client: OkHttpClient,
    url: String,
    body: String,
    channelId: String,
): GraphQLResponse {
    val reqBody = body.toRequestBody("application/json".toMediaType())
    val builder = Request.Builder().url(url).post(reqBody)
    if (channelId.isNotEmpty()) {
        builder.addHeader("c-cid", channelId)
    }
    return try {
        val response = client.newCall(builder.build()).execute()
        val responseBody = response.body.string()
        if (!response.isSuccessful) {
            LogCat.e("$transportId GraphQL request failed: ${response.code} body=${responseBody.take(200)}")
            GraphQLResponse(null, null, Exception("${response.code} - ${response.message}"))
        } else {
            GraphQLResponseParser.parse(responseBody)
        }
    } catch (e: Exception) {
        LogCat.e("$transportId send error: ${e.message}")
        throw TransportUnavailable(transportId, peerId, e)
    }
}

internal suspend fun executeDownloadRequest(
    transportId: String,
    peerId: String,
    client: OkHttpClient,
    url: String,
): Response {
    val req = Request.Builder().url(url).build()
    return try {
        client.newCall(req).execute()
    } catch (e: Exception) {
        LogCat.e("$transportId downloadFile error: ${e.message}")
        throw TransportUnavailable(transportId, peerId, e)
    }
}
