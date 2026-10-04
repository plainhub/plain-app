package com.ismartcoding.plain.chat.peer.transport

import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.platform.bleTransport
import com.ismartcoding.plain.platform.isBleReady
import com.ismartcoding.plain.platform.isWifiAwareSupported
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.*

object PeerTransportPrewarmHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "peerTransportPrewarmCapabilities" -> buildJsonObject {
            put("bleReady", isBleReady()); put("awareSupported", isWifiAwareSupported)
        }
        "peerTransportPrewarmScan" -> {
            val device = withTimeoutOrNull(params.getValue("timeoutMs").jsonPrimitive.long) {
                bleTransport().createScanner().scan(BleUuids.SERVICE_UUID).firstOrNull {
                    it.id == params.getValue("shortId").jsonPrimitive.content
                }
            }
            device?.let { buildJsonObject {
                put("shortId", it.id); put("awareSupported", it.awareSupported); put("awareRunning", it.awareRunning)
            } } ?: JsonNull
        }
        "peerTransportPrewarmObservation" -> {
            val id = params.getValue("id").jsonPrimitive.content
            val value = params.getValue("advertisement").jsonObject
            PeerCacher.setAwareSupported(id, value.getValue("awareSupported").jsonPrimitive.boolean)
            PeerCacher.setAwareRunning(id, value.getValue("awareRunning").jsonPrimitive.boolean)
            JsonNull
        }
        else -> error("Unknown peer prewarm operation")
    }
}
