package com.ismartcoding.plain.discover

import com.ismartcoding.plain.ble.PairingTransport
import com.ismartcoding.plain.ble.BleUuids
import kotlinx.serialization.json.*

object PairingNotificationHost {
    fun handle(params: JsonObject): JsonElement = JsonPrimitive(PairingTransport.sendNotification(
        params.getValue("address").jsonPrimitive.content, BleUuids.NEARBY_CHAR_UUID,
        params.getValue("body").jsonPrimitive.content,
    ))
}
