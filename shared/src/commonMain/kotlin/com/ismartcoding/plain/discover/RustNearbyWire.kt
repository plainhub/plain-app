package com.ismartcoding.plain.discover

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.enums.NearbyMessageType
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*

object RustNearbyWire {
    suspend fun encode(type: NearbyMessageType, payload: String): String = call(buildJsonObject {
        put("action", "encode")
        put("message", buildJsonObject {
            put("kind", type.name)
            if (type != NearbyMessageType.DISCOVER) put("payload", Json.parseToJsonElement(payload))
        })
    }).jsonPrimitive.content

    suspend fun parse(body: String): Pair<NearbyMessageType, String>? = try {
        val result = call(buildJsonObject { put("action", "parse"); put("body", body) }).jsonObject
        NearbyMessageType.valueOf(result.getValue("kind").jsonPrimitive.content) to (result["payload"]?.toString() ?: "")
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { null }

    suspend fun discoverReply(payload: String, shortId: String): DDiscoverReply = JsonHelper.jsonDecode(call(buildJsonObject {
        put("action", "discoverReply"); put("payload", payload); put("short_id", shortId)
    }).toString())

    private suspend fun call(body: JsonObject) = RustContentApi.postJson("chat/nearby", body).getValue("result")
}
