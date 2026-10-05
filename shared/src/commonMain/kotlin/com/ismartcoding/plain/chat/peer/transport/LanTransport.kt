package com.ismartcoding.plain.chat.peer.transport

import com.ismartcoding.plain.platform.createCryptoClient
import com.ismartcoding.plain.platform.createDownloadClient
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.chat.peer.RustPeerStore
import com.ismartcoding.plain.db.getFileUrl

object LanTransport : PeerTransport {
    override val type = PeerTransportType.LAN

    override suspend fun send(peer: DPeer, request: SignedRequest, keyBytes: ByteArray): GraphQLResponse {
        val address = RustPeerStore.address(peer)
        val client = createCryptoClient(keyBytes, 10)
        return executeGraphQLRequest(
            transportType = type,
            peerId = peer.id,
            client = client,
            url = address.apiUrl,
            body = request.body,
            channelId = request.channelId,
        )
    }

    override suspend fun downloadFile(peer: DPeer, fileId: String): DownloadedResponse {
        val client = createDownloadClient()
        return executeDownloadRequest(type, peer.id, client, peer.getFileUrl(fileId))
    }
}
