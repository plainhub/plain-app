package com.ismartcoding.plain.discover

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.data.*
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.getDeviceIP4sWithPrefixLength
import kotlinx.serialization.json.*

object RustPairingRuntime {
    suspend fun startLan(device: DNearbyDevice) = call(buildJsonObject {
        put("action", "startLan"); put("device", RustPairingStore.deviceFacts())
        put("target", buildJsonObject {
            put("deviceId", device.id); put("deviceName", device.name); put("deviceIp", ""); put("devicePort", device.port)
        })
        put("ips", JsonArray(device.ips.map(::JsonPrimitive)))
        put("interfaces", JsonArray(getDeviceIP4sWithPrefixLength().map { (ip, prefix) -> buildJsonObject { put("ip", ip); put("prefixLength", prefix.toInt()) } }))
    })
    suspend fun cancel(id: String) = call(buildJsonObject { put("action", "cancel"); put("id", id); put("generation", JsonNull) })
    suspend fun receiveRequest(request: DPairingRequest, address: String, ble: Boolean) = call(buildJsonObject {
        put("action", "receiveRequest"); put("request", Json.parseToJsonElement(JsonHelper.jsonEncode(request))); put("address", address); put("ble", ble)
    })
    suspend fun currentRequest(id: String, signature: String): Boolean = call(buildJsonObject {
        put("action", "pendingRequest"); put("id", id); put("signature", signature)
    }).jsonPrimitive.boolean
    suspend fun receiveCancel(cancel: DPairingCancel) = call(buildJsonObject {
        put("action", "receiveCancel"); put("cancel", Json.parseToJsonElement(JsonHelper.jsonEncode(cancel)))
    })
    suspend fun complete(response: DPairingResponse, senderIp: String): JsonElement = call(buildJsonObject {
        put("action", "complete"); put("response", Json.parseToJsonElement(JsonHelper.jsonEncode(response))); put("sender_ip", senderIp)
    })
    suspend fun respond(request: DPairingRequest, accepted: Boolean) = call(buildJsonObject {
        put("action", "respond"); put("request", Json.parseToJsonElement(JsonHelper.jsonEncode(request))); put("accepted", accepted); put("device", RustPairingStore.deviceFacts())
    })
    private suspend fun call(body: JsonObject) = RustContentApi.postJson("chat/pairing", body, longRunning = true).getValue("result")
}
