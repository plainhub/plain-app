package com.ismartcoding.plain.discover

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.ble.BleServiceData
import kotlinx.serialization.json.*

object RustBleServiceData {
    private suspend fun call(body: JsonObject) = RustContentApi.postJson("chat/discovery", body).getValue("result")
    suspend fun decode(data: ByteArray?): BleServiceData.Parts? {
        val row = call(buildJsonObject {
            put("action", "bleDecode")
            put("payload", data?.let { buildJsonArray { it.forEach { byte -> add(byte.toInt() and 255) } } } ?: JsonNull)
        })
        if (row == JsonNull) return null
        return row.jsonObject.let {
            BleServiceData.Parts(it.getValue("shortId").jsonPrimitive.content, it.getValue("awareSupported").jsonPrimitive.boolean, it.getValue("awareRunning").jsonPrimitive.boolean)
        }
    }
    suspend fun shortIdOf(clientId: String): String = call(buildJsonObject {
        put("action", "bleShortId"); put("id", clientId)
    }).jsonPrimitive.content
}
