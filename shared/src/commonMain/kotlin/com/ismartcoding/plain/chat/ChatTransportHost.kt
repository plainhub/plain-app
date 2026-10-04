package com.ismartcoding.plain.chat

import com.ismartcoding.plain.chat.peer.RustPeerStore
import com.ismartcoding.plain.chat.peer.transport.PeerTransportRouter
import com.ismartcoding.plain.chat.peer.transport.SignedRequest
import com.ismartcoding.plain.helpers.Base64Lenient
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*

object ChatTransportHost {
    suspend fun handle(params: JsonObject): JsonElement = withTimeout(20_000L) {
        val response = PeerTransportRouter.send(
            RustPeerStore.decode(params.getValue("peer")),
            SignedRequest(params.getValue("body").jsonPrimitive.content, params.getValue("channelId").jsonPrimitive.content),
            Base64Lenient.decode(params.getValue("key").jsonPrimitive.content),
        )
        response.exception?.let { throw it }
        buildJsonObject {
            put("data", response.data?.let(Json::parseToJsonElement) ?: JsonNull)
            put("errors", response.errors?.let { errors -> JsonArray(errors.map { buildJsonObject { put("message", it.message) } }) } ?: JsonNull)
        }
    }
}
