package com.ismartcoding.plain.chat.peer.transport

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.isWifiAwareSupported
import com.ismartcoding.plain.platform.isBleReady
import kotlinx.serialization.json.*

object PeerTransportHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement {
        if (method.startsWith("peerTransportPrewarm")) return PeerTransportPrewarmHost.handle(method, params)
        if (method == "peerTransportCapabilities") return JsonHelper.jsonEncodeToElement(buildList {
            if (isWifiAwareSupported) add(PeerTransportType.AWARE)
            if (isBleReady()) add(PeerTransportType.BLE)
        })
        if (method.startsWith("peerTransportSocket")) return PeerSocketHost.handle(method, params)
        if (method == "peerTransportBleExchange") return BleTransport.exchange(params)
        error("Unknown peer SDK operation")
    }
}
