package com.ismartcoding.plain.chat.peer.transport

import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.chat.peer.RustPeerStore
import com.ismartcoding.plain.helpers.Base64Lenient
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.*

object PeerTransportHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement {
        if (method == "peerTransportCapabilities") return PeerTransportRouter.capabilities()
        check(method == "peerTransportAttempt") { "Unknown peer transport operation" }
        val peer = RustPeerStore.decode(params.getValue("peer"))
        val type = PeerTransportType.valueOf(params.getValue("transport").jsonPrimitive.content)
        PeerCacher.setCurrentTransport(peer.id, type)
        try {
            val response = withTimeoutOrNull(params.getValue("timeoutMs").jsonPrimitive.long) {
                PeerTransportRouter.adapter(type).send(peer,
                    SignedRequest(params.getValue("body").jsonPrimitive.content, params.getValue("channelId").jsonPrimitive.content),
                    Base64Lenient.decode(params.getValue("key").jsonPrimitive.content))
            } ?: return unavailable("Platform transport attempt timed out")
            val errors = response.errors.orEmpty().map { it.message } + listOfNotNull(response.exception?.let { it.message ?: "Invalid peer response" })
            return buildJsonObject {
                put("kind", "connected")
                put("response", buildJsonObject {
                    put("data", response.data?.let(Json::parseToJsonElement) ?: JsonNull)
                    put("errors", if (errors.isEmpty()) JsonNull else JsonArray(errors.map { message -> buildJsonObject { put("message", message) } }))
                })
            }
        } catch (failure: TransportUnavailable) {
            return unavailable(failure.cause?.message ?: failure.message ?: "Transport unavailable")
        } finally { PeerCacher.setCurrentTransport(peer.id, null) }
    }
    private fun unavailable(error: String): JsonObject = buildJsonObject { put("kind", "unavailable"); put("error", error) }
}
