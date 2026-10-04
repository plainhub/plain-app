package com.ismartcoding.plain.discover

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.callChatStore
import com.ismartcoding.plain.chat.peer.RustPeerStore
import com.ismartcoding.plain.data.*
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.getDeviceIP4s
import com.ismartcoding.plain.platform.getDeviceType
import com.ismartcoding.plain.platform.isWifiAwareSupported
import com.ismartcoding.plain.preferences.UserPrefs
import kotlinx.serialization.json.*

object RustPairingStore {
    suspend fun tickets(): List<DPairingTicket> = callChatStore("pairingTickets") {}.jsonArray.map(::ticket)
    internal fun deviceFacts() = buildJsonObject {
        put("name", TempData.deviceName.value)
        put("port", UserPrefs.httpsPort.value)
        put("device_type", getDeviceType().name)
        put("ips", JsonArray(getDeviceIP4s().map(::JsonPrimitive)))
        put("aware_supported", isWifiAwareSupported)
    }
    suspend fun start(id: String, name: String, ip: String, port: Int): Pair<DPairingRequest, DPairingTicket> {
        val result = callChatStore("startPairing") {
            put("device", deviceFacts())
            put("target", buildJsonObject {
                put("deviceId", id); put("deviceName", name); put("deviceIp", ip); put("devicePort", port)
            })
        }.jsonObject
        return JsonHelper.jsonDecode<DPairingRequest>(result.getValue("request").toString()) to ticket(result.getValue("ticket"))
    }
    suspend fun complete(response: DPairingResponse, senderIp: String): DPairingOutcome? {
        val result = callChatStore("completePairing") {
            put("response", Json.parseToJsonElement(JsonHelper.jsonEncode(response)))
            put("sender_ip", senderIp)
        }.takeUnless { it is JsonNull }?.jsonObject ?: return null
        return DPairingOutcome(ticket(result.getValue("ticket")), result.getValue("peer").takeUnless { it is JsonNull }?.let(RustPeerStore::decode), result.getValue("error").jsonPrimitive.content)
    }
    suspend fun respond(request: DPairingRequest, accepted: Boolean): Pair<DPairingResponse, com.ismartcoding.plain.db.DPeer?>? {
        val result = callChatStore("respondPairing") {
            put("request", Json.parseToJsonElement(JsonHelper.jsonEncode(request)))
            put("accepted", accepted)
            put("device", deviceFacts())
        }.takeUnless { it is JsonNull }?.jsonObject ?: return null
        return JsonHelper.jsonDecode<DPairingResponse>(result.getValue("response").toString()) to result.getValue("peer").takeUnless { it is JsonNull }?.let(RustPeerStore::decode)
    }
    suspend fun receiveRequest(request: DPairingRequest): Boolean? = callChatStore("receivePairingRequest") {
        put("request", Json.parseToJsonElement(JsonHelper.jsonEncode(request)))
    }.takeUnless { it is JsonNull }?.jsonPrimitive?.boolean
    suspend fun cancel(id: String, generation: String? = null): Pair<DPairingTicket, DPairingCancel>? {
        val result = callChatStore("cancelPairing") { put("id", id); put("generation", generation?.let(::JsonPrimitive) ?: JsonNull) }
            .takeUnless { it is JsonNull }?.jsonObject ?: return null
        return ticket(result.getValue("ticket")) to JsonHelper.jsonDecode<DPairingCancel>(result.getValue("cancel").toString())
    }
    suspend fun expire(ticket: DPairingTicket): DPairingTicket? = callChatStore("expirePairing") {
        put("id", ticket.deviceId); put("generation", ticket.generation)
    }.takeUnless { it is JsonNull }?.let(::ticket)
    suspend fun receiveCancel(cancel: DPairingCancel): DPairingResult? = callChatStore("receivePairingCancel") {
        put("cancel", Json.parseToJsonElement(JsonHelper.jsonEncode(cancel)))
    }.takeUnless { it is JsonNull }?.jsonObject?.let { DPairingResult(it.getValue("deviceId").jsonPrimitive.content, it.getValue("deviceName").jsonPrimitive.content) }
    suspend fun save(id: String, name: String, ips: List<String>, port: Int, deviceType: com.ismartcoding.plain.enums.DeviceType, key: String, publicKey: String) {
        callChatStore("savePairedPeer") {
            put("facts", buildJsonObject {
                put("id", id); put("name", name); put("ips", JsonArray(ips.map(::JsonPrimitive))); put("port", port)
                put("device_type", deviceType.name); put("key", key); put("public_key", publicKey)
            })
        }
    }
    private fun ticket(value: JsonElement): DPairingTicket = JsonHelper.jsonDecode(value.toString())
}
