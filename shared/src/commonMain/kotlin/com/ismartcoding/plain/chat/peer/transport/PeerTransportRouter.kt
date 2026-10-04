package com.ismartcoding.plain.chat.peer.transport

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.chat.peer.RustPeerStore
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.platform.createWifiAwareTransport
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64

object PeerTransportRouter {
    private val transports = buildList {
        add(LanTransport)
        createWifiAwareTransport()?.let { add(it) }
        add(BleTransport)
    }.associateBy { it.type }

    internal fun capabilities(): JsonArray = JsonArray(transports.keys.map { JsonPrimitive(it.name) })
    internal fun adapter(type: PeerTransportType): PeerTransport = transports[type]
        ?: throw TransportUnavailable(type, "", IllegalStateException("Platform transport unavailable"))

    suspend fun send(peer: DPeer, request: SignedRequest, keyBytes: ByteArray): GraphQLResponse {
        val response = call(buildJsonObject {
            put("action", "send"); put("id", peer.id); put("channel_id", request.channelId)
            put("body", request.body); put("key", Base64.encode(keyBytes))
        })
        return GraphQLResponseParser.parse(response.toString())
    }

    suspend fun downloadFile(peer: DPeer, fileId: String): DownloadedResponse {
        var ticket: JsonElement? = null
        try {
            val begin = withContext(NonCancellable) {
                val value = call(buildJsonObject { put("action", "beginDownload"); put("id", peer.id); put("available", capabilities()) }).jsonObject
                ticket = value["ticket"]?.takeUnless { it is JsonNull }
                value
            }
            val currentPeer = RustPeerStore.decode(begin.getValue("peer"))
            var error = begin["error"]?.jsonPrimitive?.contentOrNull
            while (ticket != null) {
                currentCoroutineContext().ensureActive()
                val active = checkNotNull(ticket)
                val type = PeerTransportType.valueOf(active.jsonObject.getValue("transport").jsonPrimitive.content)
                val downloaded = try {
                    adapter(type).downloadFile(currentPeer, fileId)
                } catch (unavailable: TransportUnavailable) {
                    withContext(NonCancellable) {
                        val step = finish(active, buildJsonObject {
                            put("kind", "unavailable"); put("error", unavailable.cause?.message ?: unavailable.message ?: "Transport unavailable")
                        })
                        ticket = step["ticket"]?.takeUnless { it is JsonNull }
                        error = step["error"]?.jsonPrimitive?.contentOrNull
                    }
                    continue
                }
                try {
                    withContext(NonCancellable) { finish(active, buildJsonObject { put("kind", "connected") }) }
                    ticket = null
                    currentCoroutineContext().ensureActive()
                    return downloaded
                } catch (failure: Throwable) { downloaded.close(); throw failure }
            }
            throw IllegalStateException(error ?: "Peer transport unavailable")
        } finally {
            ticket?.let { active -> withContext(NonCancellable) {
                runCatching { call(buildJsonObject { put("action", "abort"); put("ticket", active) }) }
            } }
        }
    }
    private suspend fun finish(ticket: JsonElement, outcome: JsonObject): JsonObject = call(buildJsonObject {
        put("action", "finishDownload"); put("ticket", ticket); put("outcome", outcome)
    }).jsonObject
    private suspend fun call(body: JsonObject): JsonElement = RustContentApi.postJson("chat/transport", body, longRunning = true).getValue("result")
}
