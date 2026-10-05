package com.ismartcoding.plain.chat.peer.transport

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.chat.peer.RustPeerStore
import com.ismartcoding.plain.db.DPeer
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*

object LanTransport : PeerTransport {
    override val type = PeerTransportType.LAN
    override suspend fun send(peer: DPeer, request: SignedRequest, keyBytes: ByteArray): GraphQLResponse =
        error("LAN sending is owned by Rust")
    override suspend fun downloadFile(peer: DPeer, fileId: String): DownloadedResponse {
        val response = try {
            RustContentApi.postStream("chat/lan/file", buildJsonObject {
                put("id", peer.id); put("expected", RustPeerStore.encode(peer)); put("file_id", fileId)
            })
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { throw TransportUnavailable(type, peer.id, failure) }
        if (!response.isSuccess()) {
            response.close()
            throw TransportUnavailable(type, peer.id)
        }
        return DownloadedResponse(response.status.value, response.channel) { response.close() }
    }
}
