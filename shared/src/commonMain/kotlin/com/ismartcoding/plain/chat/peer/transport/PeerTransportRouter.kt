package com.ismartcoding.plain.chat.peer.transport
import com.ismartcoding.plain.platform.createWifiAwareTransport

import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.lib.logcat.LogCat

object PeerTransportRouter {
    private val transports: List<PeerTransport> = buildList {
        add(LanTransport)
        createWifiAwareTransport()?.let { add(it) }
        // BLE is the last-resort fallback for both chat and file download: it
        // works whenever the peer is paired (the peer's clientId is broadcast
        // in the BLE scan response serviceData, so a clientId-based BLE scan
        // finds the peer even when LAN and Wi-Fi Aware are both unavailable).
        // File downloads over BLE use chunked byte-range requests to stay
        // within the GATT response limits — slow but functional for
        // cross-subnet peers where no other transport is reachable.
        add(BleTransport)
    }

    suspend fun send(peer: DPeer, request: SignedRequest, keyBytes: ByteArray): GraphQLResponse {
        val errors = mutableListOf<String>()
        try {
            for (t in transports) {
                if (t is LanTransport && peer.ip.isEmpty()) {
                    continue
                }
                if (PeerCircuitBreaker.isOpen(peer.id, t.type)) {
                    LogCat.d("transport ${t.type} skipped for peer ${peer.id} (breaker open)")
                    continue
                }
                PeerCacher.setCurrentTransport(peer.id, t.type)
                try {
                    val resp = t.send(peer, request, keyBytes)
                    PeerCircuitBreaker.recordSuccess(peer.id, t.type)
                    return resp
                } catch (e: TransportUnavailable) {
                    PeerCircuitBreaker.recordFailure(peer.id, t.type)
                    val causeMsg = e.cause?.message ?: e.message
                    errors.add("${t.type.name} error: $causeMsg")
                    LogCat.d("${t.type.name} error: ${peer.id} $causeMsg")
                }
            }
            throw Exception(errors.joinToString("\n"))
        } finally {
            PeerCacher.setCurrentTransport(peer.id, null)
        }
    }

    suspend fun downloadFile(
        peer: DPeer,
        fileId: String,
    ): DownloadedResponse {
        var lastError: Throwable? = null
        for (t in transports) {
            if (PeerCircuitBreaker.isOpen(peer.id, t.type)) {
                LogCat.d("transport ${t.type} skipped for peer ${peer.id} (breaker open)")
                continue
            }
            try {
                val resp = t.downloadFile(peer, fileId)
                PeerCircuitBreaker.recordSuccess(peer.id, t.type)
                return resp
            } catch (e: TransportUnavailable) {
                PeerCircuitBreaker.recordFailure(peer.id, t.type)
                val causeMsg = e.cause?.message ?: e.message
                LogCat.d("transport ${t.type} unavailable for peer ${peer.id}: $causeMsg")
                lastError = e
            }
        }
        throw Exception("all transports exhausted for file download peer=${peer.id}", lastError)
    }
}
