package com.ismartcoding.plain.chat.peer.transport

import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.lib.logcat.LogCat

object PeerTransportRouter {
    private val transports: List<PeerTransport> = buildList {
        add(LanTransport)
        if (WifiAwareTransport.isSupported()) {
            add(WifiAwareTransport)
        }
    }

    suspend fun send(peer: DPeer, request: SignedRequest, keyBytes: ByteArray): GraphQLResponse {
        for (t in transports) {
            if (t is LanTransport && peer.ip.isEmpty()) {
                continue
            }
            if (PeerCircuitBreaker.isOpen(peer.id, t.id)) {
                LogCat.d("transport ${t.id} skipped for peer ${peer.id} (breaker open)")
                continue
            }
            try {
                val resp = t.send(peer, request, keyBytes)
                PeerCircuitBreaker.recordSuccess(peer.id, t.id)
                return resp
            } catch (e: TransportUnavailable) {
                PeerCircuitBreaker.recordFailure(peer.id, t.id)
                LogCat.d("transport ${t.id} unavailable for peer ${peer.id}: ${e.message}")
            }
        }
        throw Exception("all transports exhausted for peer ${peer.id}")
    }

    suspend fun downloadFile(
        peer: DPeer,
        fileId: String,
    ): DownloadedResponse {
        var lastError: Throwable? = null
        for (t in transports) {
            if (PeerCircuitBreaker.isOpen(peer.id, t.id)) {
                LogCat.d("transport ${t.id} skipped for peer ${peer.id} (breaker open)")
                continue
            }
            try {
                val resp = t.downloadFile(peer, fileId)
                PeerCircuitBreaker.recordSuccess(peer.id, t.id)
                return resp
            } catch (e: TransportUnavailable) {
                PeerCircuitBreaker.recordFailure(peer.id, t.id)
                LogCat.d("transport ${t.id} unavailable for peer ${peer.id}: ${e.message}")
                lastError = e
            }
        }
        throw Exception("all transports exhausted for file download peer=${peer.id}", lastError)
    }
}


