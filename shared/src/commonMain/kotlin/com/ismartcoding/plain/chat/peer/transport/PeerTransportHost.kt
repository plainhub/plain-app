package com.ismartcoding.plain.chat.peer.transport

import kotlinx.serialization.json.*

object PeerTransportHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement {
        if (method.startsWith("peerTransportPrewarm")) return PeerTransportPrewarmHost.handle(method, params)
        if (method == "peerTransportCapabilities") return PeerTransportRouter.capabilities()
        if (method.startsWith("peerTransportSocket")) return PeerSocketHost.handle(method, params)
        if (method == "peerTransportBleExchange") return BleTransport.exchange(params)
        error("Unknown peer SDK operation")
    }
}
