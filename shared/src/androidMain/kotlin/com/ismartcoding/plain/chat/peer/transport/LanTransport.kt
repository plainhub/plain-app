package com.ismartcoding.plain.chat.peer.transport

import com.ismartcoding.plain.api.OkHttpClientFactory
import com.ismartcoding.plain.api.applyDownloadConfig
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.getApiUrl
import com.ismartcoding.plain.db.getFileUrl

object LanTransport : PeerTransport {
    override val id: String = "lan"

    override suspend fun send(peer: DPeer, request: SignedRequest, keyBytes: ByteArray): GraphQLResponse {
        val client = OkHttpClientFactory.createCryptoHttpClient(keyBytes, 10)
        return executeGraphQLRequest(
            transportId = id,
            peerId = peer.id,
            client = client,
            url = peer.getApiUrl(),
            body = request.body,
            channelId = request.channelId,
        )
    }

    override suspend fun downloadFile(peer: DPeer, fileId: String): DownloadedResponse {
        val base = OkHttpClientFactory.createUnsafeOkHttpClient()
        val client = base.newBuilder()
            .applyDownloadConfig()
            .build()
        val response = executeDownloadRequest(id, peer.id, client, peer.getFileUrl(fileId))
        return DownloadedResponse(client, response)
    }
}


