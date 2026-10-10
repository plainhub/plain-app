package com.ismartcoding.plain.discover

import com.ismartcoding.plain.ui.models.NearbyItemStatus
import com.ismartcoding.plain.ui.models.NearbyViewModel
import kotlinx.serialization.json.*

// Rust broadcasts pairing events to both mobile and web clients; projections must not republish them.
object PairingProjection {
    suspend fun reconcile() {
        val states = RustPairingRuntime.states().map { it.jsonObject }
        val active = states.map { it.getValue("deviceId").jsonPrimitive.content }.toSet()
        NearbyViewModel.itemStatus.toMap().forEach { (id, status) ->
            if (status in setOf(NearbyItemStatus.STARTING, NearbyItemStatus.PAIRING) && id !in active) NearbyViewModel.itemStatus.remove(id)
        }
        states.forEach {
            NearbyViewModel.itemStatus[it.getValue("deviceId").jsonPrimitive.content] = NearbyItemStatus.valueOf(it.getValue("phase").jsonPrimitive.content)
        }
    }

    suspend fun started(payload: String) {
        val value = Json.parseToJsonElement(payload).jsonObject
        val id = value.getValue("deviceId").jsonPrimitive.content
        if (RustPairingStore.tickets().none { it.deviceId == id && it.generation == value.getValue("generation").jsonPrimitive.content }) return
        NearbyViewModel.itemStatus[id] = NearbyItemStatus.PAIRING
    }

    suspend fun request(payload: String) {
        val request = com.ismartcoding.plain.lib.JsonHelper.jsonDecode<com.ismartcoding.plain.data.DPairingRequest>(payload)
        if (!RustPairingRuntime.currentRequest(request.fromId, request.signature)) return
        com.ismartcoding.plain.lib.sendEvent(com.ismartcoding.plain.events.HPairingRequestReceivedEvent(request))
    }

    suspend fun canceled(payload: String) {
        val value = Json.parseToJsonElement(payload).jsonObject
        val id = value.getValue("deviceId").jsonPrimitive.content
        if (RustPairingStore.tickets().any { it.deviceId == id }) return
        NearbyViewModel.itemStatus.remove(id)
        com.ismartcoding.plain.lib.sendEvent(com.ismartcoding.plain.events.HPairingCanceledEvent(id))
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
        NearbyViewModel.itemStatus.remove(id)
    }
}
