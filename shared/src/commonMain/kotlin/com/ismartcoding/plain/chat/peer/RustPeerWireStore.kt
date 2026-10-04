package com.ismartcoding.plain.chat.peer

import com.ismartcoding.plain.chat.callChatStore
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64

object RustPeerWireStore {
    suspend fun chat(peerId: String, content: String, channelId: String = ""): JsonObject = prepare(peerId, buildJsonObject {
        put("kind", "chat"); put("content", content); put("channel_id", channelId)
    })
    suspend fun startAware(peerId: String): JsonObject = prepare(peerId, buildJsonObject { put("kind", "startAware") })
    private suspend fun prepare(id: String, operation: JsonObject) = callChatStore("preparePeer") {
        put("id", id); put("operation", operation)
    }.jsonObject
    suspend fun encrypt(key: ByteArray, body: String): ByteArray = Base64.decode(callChatStore("encryptPeer") {
        put("key", Base64.encode(key)); put("body", body)
    }.jsonPrimitive.content)
    suspend fun decrypt(key: ByteArray, body: ByteArray): String? = callChatStore("decryptPeer") {
        put("key", Base64.encode(key)); put("body", Base64.encode(body))
    }.takeUnless { it is JsonNull }?.jsonPrimitive?.content
}
