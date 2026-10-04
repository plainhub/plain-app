package com.ismartcoding.plain.chat.peer

import com.ismartcoding.plain.platform.startAwareIfNeeded
import com.ismartcoding.plain.platform.subscribeAwareForPeer
import kotlinx.serialization.json.*

object PeerGraphQLHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "peerDeviceInfo" -> buildJsonObject {
            put("name", com.ismartcoding.plain.TempData.deviceName.value)
            put("deviceType", com.ismartcoding.plain.platform.getDeviceType().name)
        }
        "peerStartAware" -> {
            val peer = RustPeerStore.decode(params.getValue("peer"))
            val started = startAwareIfNeeded()
            if (started) subscribeAwareForPeer(peer)
            JsonPrimitive(started)
        }
        else -> error("Unknown peer Host operation: $method")
    }
}
