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

    suspend fun timeout(payload: String) {
        val value = Json.parseToJsonElement(payload).jsonObject
        val id = value.getValue("deviceId").jsonPrimitive.content
        if (RustPairingStore.tickets().any { it.deviceId == id }) return
        PairingCore.notifyFailed(id, value.getValue("deviceName").jsonPrimitive.content,
            value.getValue("error").jsonPrimitive.content)
    }
}
