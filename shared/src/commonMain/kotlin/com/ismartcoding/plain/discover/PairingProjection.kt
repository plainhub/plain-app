package com.ismartcoding.plain.discover

import com.ismartcoding.plain.ui.models.NearbyItemStatus
import com.ismartcoding.plain.ui.models.NearbyViewModel
import kotlinx.serialization.json.*

object PairingProjection {
    suspend fun reconcile() {
        val active = RustPairingStore.tickets().map { it.deviceId }.toSet()
        NearbyViewModel.itemStatus.toMap().forEach { (id, status) ->
            if (status == NearbyItemStatus.PAIRING && id !in active) NearbyViewModel.itemStatus.remove(id)
        }
    }

    suspend fun started(payload: String) {
        val value = Json.parseToJsonElement(payload).jsonObject
        val id = value.getValue("deviceId").jsonPrimitive.content
        if (RustPairingStore.tickets().none { it.deviceId == id && it.generation == value.getValue("generation").jsonPrimitive.content }) return
        NearbyViewModel.onPairingRequestSent(id)
        com.ismartcoding.plain.lib.sendEvent(com.ismartcoding.plain.events.WebSocketEvent(
            com.ismartcoding.plain.events.EventType.PAIRING_STARTED, com.ismartcoding.plain.lib.JsonHelper.jsonEncode(
                com.ismartcoding.plain.data.DPairingResult(id, value.getValue("deviceName").jsonPrimitive.content))))
    }

    suspend fun request(payload: String) {
        val request = com.ismartcoding.plain.lib.JsonHelper.jsonDecode<com.ismartcoding.plain.data.DPairingRequest>(payload)
        if (!RustPairingRuntime.currentRequest(request.fromId, request.signature)) return
        com.ismartcoding.plain.lib.sendEvent(com.ismartcoding.plain.events.PairingRequestReceivedEvent(request))
        com.ismartcoding.plain.lib.sendEvent(com.ismartcoding.plain.events.WebSocketEvent(com.ismartcoding.plain.events.EventType.PAIRING_REQUEST_RECEIVED, payload))
    }

    suspend fun canceled(payload: String) {
        val value = Json.parseToJsonElement(payload).jsonObject
        val id = value.getValue("deviceId").jsonPrimitive.content
        if (RustPairingStore.tickets().any { it.deviceId == id }) return
        NearbyViewModel.itemStatus.remove(id)
        com.ismartcoding.plain.lib.sendEvent(com.ismartcoding.plain.events.PairingCanceledEvent(id))
        com.ismartcoding.plain.lib.sendEvent(com.ismartcoding.plain.events.WebSocketEvent(com.ismartcoding.plain.events.EventType.PAIRING_CANCELED,
            com.ismartcoding.plain.lib.JsonHelper.jsonEncode(com.ismartcoding.plain.data.DPairingResult(id, value.getValue("deviceName").jsonPrimitive.content))))
    }

    suspend fun success(payload: String) {
        val value = Json.parseToJsonElement(payload).jsonObject
        val peer = com.ismartcoding.plain.chat.peer.RustPeerStore.getById(value.getValue("deviceId").jsonPrimitive.content) ?: return
        if (!peer.isPaired() || peer.key != value.getValue("key").jsonPrimitive.content) return
        PairingCore.publishSuccess(peer.id, peer.name, value.getValue("ip").jsonPrimitive.content, peer.key)
    }

    suspend fun timeout(payload: String) {
        val value = Json.parseToJsonElement(payload).jsonObject
        val id = value.getValue("deviceId").jsonPrimitive.content
        if (value["generation"] != null && RustPairingStore.tickets().any { it.deviceId == id }) return
        PairingCore.notifyFailed(id, value.getValue("deviceName").jsonPrimitive.content,
            value.getValue("error").jsonPrimitive.content)
    }
}
