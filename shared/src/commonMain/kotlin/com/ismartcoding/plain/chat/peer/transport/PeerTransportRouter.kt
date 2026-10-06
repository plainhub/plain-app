package com.ismartcoding.plain.chat.peer.transport

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.db.DPeer
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64

object PeerTransportRouter {
    internal fun capabilities(): JsonArray = JsonArray(buildList {
        if (com.ismartcoding.plain.platform.isWifiAwareSupported) add(JsonPrimitive(PeerTransportType.AWARE.name))
        if (com.ismartcoding.plain.platform.isBleReady()) add(JsonPrimitive(PeerTransportType.BLE.name))
    })

    suspend fun send(peer: DPeer, request: SignedRequest, keyBytes: ByteArray): GraphQLResponse {
        val response = call(buildJsonObject {
            put("action", "send"); put("id", peer.id); put("channel_id", request.channelId)
            put("body", request.body); put("key", Base64.encode(keyBytes))
        })
        return GraphQLResponseParser.parse(response.toString())
    }

    private suspend fun call(body: JsonObject): JsonElement = RustContentApi.postJsonOrThrow("chat/transport", body, longRunning = true).getValue("result")
}
