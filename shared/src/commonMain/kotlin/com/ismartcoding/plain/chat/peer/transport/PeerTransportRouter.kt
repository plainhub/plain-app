package com.ismartcoding.plain.chat.peer.transport
import com.ismartcoding.plain.platform.createWifiAwareTransport

import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.lib.logcat.LogCat

object PeerTransportRouter {
    private val transports: List<PeerTransport> = buildList {
        add(LanTransport)
        createWifiAwareTransport()?.let { add(it) }
        // BLE is the last-resort fallback for chat: it works whenever the peer
        // is paired (the peer's clientId is broadcast in the BLE scan response
        // serviceData, so a clientId-based BLE scan finds the peer even when
        // LAN and Wi-Fi Aware are both unavailable). File download still goes
        // through LAN since BLE throughput is too low for media.
        add(BleTransport)
    }

    suspend fun send(peer: DPeer, request: SignedRequest, keyBytes: ByteArray): GraphQLResponse {
        val errors = mutableListOf<String>()
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
                val causeMsg = e.cause?.message ?: e.message
                errors.add("${t.id.uppercase()} error: $causeMsg")
                LogCat.d("${t.id.uppercase()} error: ${peer.id} $causeMsg")
            }
        }
        throw Exception(errors.joinToString("\n"))
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
                val causeMsg = e.cause?.message ?: e.message
                LogCat.d("transport ${t.id} unavailable for peer ${peer.id}: $causeMsg")
                lastError = e
            }
        }
        throw Exception("all transports exhausted for file download peer=${peer.id}", lastError)
    }
}
