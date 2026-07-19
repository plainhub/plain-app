package com.ismartcoding.plain.chat.peer

import com.ismartcoding.plain.helpers.SignatureHelper
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.chat.peer.transport.PeerTransportRouter
import com.ismartcoding.plain.chat.peer.transport.SignedRequest
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.toJSONString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object PeerGraphQLClient {
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

    /**
     * Sends a `startAware` mutation to [peer] via the transport fallback chain
     * (typically BLE). Asks the peer to start its Wi-Fi Aware service and
     * subscribe for us so both sides can establish an Aware data path for
     * subsequent messages. Fire-and-forget — callers don't need to wait for
     * the response, but we return it so PeerTransportPrewarmer can log the
     * result.
     */
    suspend fun startAware(peer: DPeer): GraphQLResponse {
        val mutation = $$"""
            mutation StartAware {
                startAware
            }
        """.trimIndent()

        val keyBytes = requireNotNull(PeerCacher.getKeyBytes(peer.id)) {
            "PeerCacher has no key bytes for peer ${peer.id}"
        }
        val request = buildSignedRequest(
            query = mutation,
            variables = emptyMap(),
            channelId = "",
        )
        return PeerTransportRouter.send(peer, request, keyBytes)
    }

    private suspend fun buildSignedRequest(
        query: String,
        variables: Map<String, String>,
        channelId: String,
    ): SignedRequest {
        val requestJson = buildJsonObject {
            put("query", query)
            put("variables", buildJsonObject {
                variables.forEach { (key, value) -> put(key, value) }
            })
        }.toString()

        val timestamp = TimeHelper.nowMillis().toString()
        val signature = SignatureHelper.signTextAsync("$timestamp$requestJson")
        val body = "$signature|$timestamp|$requestJson"
        return SignedRequest(body = body, channelId = channelId)
    }
}
