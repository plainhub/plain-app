package com.ismartcoding.plain.tests

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64

@OptIn(ExperimentalSerializationApi::class)
internal object PeerWireTestApi {
    suspend fun chat(peerId: String, content: String, channelId: String = ""): JsonObject =
        call(Request.Prepare(peerId, Operation.Chat(content, channelId))).jsonObject
    suspend fun startAware(peerId: String): JsonObject = call(Request.Prepare(peerId, Operation.StartAware)).jsonObject
    suspend fun encrypt(key: ByteArray, body: String): ByteArray =
        Base64.decode(call(Request.Encrypt(Base64.encode(key), body)).jsonPrimitive.content)
    suspend fun decrypt(key: ByteArray, body: ByteArray): String? =
        call(Request.Decrypt(Base64.encode(key), Base64.encode(body))).takeUnless { it is JsonNull }?.jsonPrimitive?.content
    suspend fun authenticatePeer(fromId: String, channelId: String, body: ByteArray): JsonObject =
        call(Request.AuthenticatePeer(fromId, channelId, Base64.encode(body))).jsonObject
    suspend fun authenticateEnvelope(key: ByteArray, publicKey: ByteArray, body: ByteArray): JsonObject =
        call(Request.AuthenticateEnvelope(Base64.encode(key), Base64.encode(publicKey), Base64.encode(body))).jsonObject
    private suspend fun call(request: Request): JsonElement = RustContentApi.postJsonOrThrow(
        "chat/store", JsonHelper.jsonEncodeToElement(request).jsonObject,
    ).getValue("result")

    @Serializable @JsonClassDiscriminator("action")
    private sealed class Request {
        @Serializable @SerialName("preparePeer") data class Prepare(val id: String, val operation: Operation) : Request()
        @Serializable @SerialName("encryptPeer") data class Encrypt(val key: String, val body: String) : Request()
        @Serializable @SerialName("decryptPeer") data class Decrypt(val key: String, val body: String) : Request()
        @Serializable @SerialName("authenticatePeer") data class AuthenticatePeer(
            @SerialName("from_id") val fromId: String, @SerialName("channel_id") val channelId: String, val body: String,
        ) : Request()
        @Serializable @SerialName("authenticateEnvelope") data class AuthenticateEnvelope(
            val key: String, @SerialName("public_key") val publicKey: String, val body: String,
        ) : Request()
    }
    @Serializable @JsonClassDiscriminator("kind")
    private sealed class Operation {
        @Serializable @SerialName("chat") data class Chat(val content: String, @SerialName("channel_id") val channelId: String) : Operation()
        @Serializable @SerialName("startAware") data object StartAware : Operation()
    }
}
