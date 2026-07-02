package com.ismartcoding.plain.chat.peer

import com.ismartcoding.plain.helpers.SignatureHelper
import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.chat.peer.transport.PeerTransportRouter
import com.ismartcoding.plain.chat.peer.transport.SignedRequest
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.toJSONString
import org.json.JSONObject

data class GraphQLResponse(
    val data: JSONObject? = null,
    val errors: List<GraphQLError>? = null,
    val exception: Throwable? = null,
) {
    val isSuccess: Boolean = errors.isNullOrEmpty() || exception == null

    fun getError(): String {
        if (errors?.isNotEmpty() == true) {
            return errors.joinToString(", ") { it.message }
        }

        return exception?.message ?: "Unknown error"
    }
}

data class GraphQLError(
    val message: String
)

object PeerGraphQLClient {
    /**
     * Send a chat message to a paired peer (peer-to-peer chat).
     * Uses [PeerCacher.getKeyBytes] for encryption.
     */
    suspend fun createChatItem(
        peer: DPeer,
        clientId: String,
        content: DMessageContent,
    ): GraphQLResponse {
        val mutation = $$"""
                mutation CreateChatItem($content: String!) {
                    createChatItem(content: $content) {
                        id
                        fromId
                        toId
                        createdAt
                    }
                }
            """.trimIndent()

        val request = buildSignedRequest(
            query = mutation,
            variables = mapOf("content" to content.toJSONString()),
            channelId = "",
        )
        val keyBytes = requireNotNull(PeerCacher.getKeyBytes(peer.id)) {
            "PeerCacher has no key bytes for peer ${peer.id}"
        }
        return PeerTransportRouter.send(peer, request, keyBytes)
    }

    /**
     * Send a channel system message to a peer.
     * If the peer is paired (peer.key is non-empty), uses [PeerCacher.getKeyBytes].
     * Otherwise, uses [ChannelCacher.getKeyBytes] with c-cid header for non-paired channel members.
     */
    suspend fun sendChannelSystemMessage(
        peer: DPeer,
        clientId: String,
        type: String,
        payload: String,
        channelId: String = "",
    ): GraphQLResponse {
        val mutation = $$"""
                mutation ChannelSystemMessage($type: String!, $payload: String!) {
                    channelSystemMessage(type: $type, payload: $payload)
                }
            """.trimIndent()

        val variables = mapOf(
            "type" to type,
            "payload" to payload,
        )

        val keyBytes = if (peer.key.isNotEmpty()) {
            requireNotNull(PeerCacher.getKeyBytes(peer.id)) {
                "PeerCacher has no key bytes for peer ${peer.id}"
            }
        } else {
            requireNotNull(ChannelCacher.getKeyBytes(channelId)) {
                "ChannelCacher has no key bytes for channel $channelId"
            }
        }

        val request = buildSignedRequest(
            query = mutation,
            variables = variables,
            channelId = channelId,
        )
        return PeerTransportRouter.send(peer, request, keyBytes)
    }

    /**
     * Send a chat message to a channel member.
     * Uses [ChannelCacher.getKeyBytes] for ChaCha20 encryption and includes [channelId] via the c-cid header
     * so the receiver can look up the correct decryption key and member public keys.
     */
    suspend fun createChannelChatItem(
        peer: DPeer,
        channelId: String,
        content: DMessageContent,
    ): GraphQLResponse {
        val mutation = $$"""
                mutation CreateChatItem($content: String!) {
                    createChatItem(content: $content) {
                        id
                        fromId
                        toId
                        createdAt
                    }
                }
            """.trimIndent()

        val keyBytes = requireNotNull(ChannelCacher.getKeyBytes(channelId)) {
            "ChannelCacher has no key bytes for channel $channelId"
        }

        val request = buildSignedRequest(
            query = mutation,
            variables = mapOf("content" to content.toJSONString()),
            channelId = channelId,
        )
        return PeerTransportRouter.send(peer, request, keyBytes)
    }

    private suspend fun buildSignedRequest(
        query: String,
        variables: Map<String, String>,
        channelId: String,
    ): SignedRequest {
        val requestJson = JSONObject().apply {
            put("query", query)
            val variablesJson = JSONObject()
            variables.forEach { (key, value) ->
                variablesJson.put(key, value)
            }
            put("variables", variablesJson)
        }.toString()

        val timestamp = System.currentTimeMillis().toString()
        val signature = SignatureHelper.signTextAsync("$timestamp$requestJson")
        val body = "$signature|$timestamp|$requestJson"
        return SignedRequest(body = body, channelId = channelId)
    }
}
