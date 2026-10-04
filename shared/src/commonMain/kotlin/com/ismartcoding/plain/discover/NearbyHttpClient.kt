package com.ismartcoding.plain.discover

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.enums.NearbyMessageType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*

object NearbyHttpClient {
    suspend fun send(type: NearbyMessageType, payload: String, targetIp: String, targetPort: Int): Boolean =
        call(buildJsonObject {
            put("action", "send"); put("ip", targetIp); put("port", targetPort)
            put("message", buildJsonObject { put("kind", type.name); put("payload", Json.parseToJsonElement(payload)) })
        })

    suspend fun probe(targetIp: String, targetPort: Int): Boolean =
        call(buildJsonObject {
            put("action", "probe"); put("ip", targetIp); put("port", targetPort)
        })

    private suspend fun call(body: JsonObject): Boolean = try {
        RustContentApi.postJson("chat/nearby", body).getValue("result").jsonPrimitive.boolean
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { false }
}
