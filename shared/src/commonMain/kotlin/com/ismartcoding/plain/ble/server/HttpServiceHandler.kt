package com.ismartcoding.plain.ble.server

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.ble.BleRequestData
import com.ismartcoding.plain.ble.BleUuids
import kotlinx.serialization.json.*

class HttpServiceHandler : BleServiceHandler {
    override val charUuid: String = BleUuids.HTTP_CHAR_UUID
    override suspend fun handleRequest(requestData: BleRequestData, clientMac: String): String =
        RustContentApi.postJsonOrThrow("chat/ble-http", buildJsonObject {
            put("body", requestData.body)
            put("remote_host", clientMac)
            put("headers", JsonObject(requestData.headers.mapValues { JsonPrimitive(it.value) }))
        }, longRunning = true).getValue("result").jsonPrimitive.content
}
