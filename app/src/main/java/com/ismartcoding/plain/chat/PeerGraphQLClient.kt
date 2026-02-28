package com.ismartcoding.plain.chat

import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.toJSONString
import com.ismartcoding.plain.helpers.SignatureHelper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class GraphQLResponse(
    val data: JSONObject? = null,
    val errors: List<GraphQLError>? = null,
    val isSuccess: Boolean = false
)

data class GraphQLError(
    val message: String
)

object PeerGraphQLClient {
    /**
     * Send a chat message to a paired peer (peer-to-peer chat).
     * Uses [DPeer.key] for encryption.
     */
    suspend fun createChatItem(
        peer: DPeer,
        clientId: String,
        content: DMessageContent,
    ): GraphQLResponse? {
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

        return execute(
            peer = peer,
            clientId = clientId,
            query = mutation,
            variables = mapOf("content" to content.toJSONString())
        )
    }

    /**
     * Send a channel system message to a peer.
     * If the peer is paired (peer.key is non-empty), uses the peer's shared key.
     * Otherwise, uses [channelKey] with c-cid header for non-paired channel members.
     */
    suspend fun sendChannelSystemMessage(
        peer: DPeer,
        clientId: String,
        type: String,
        payload: String,
        channelId: String = "",
        channelKey: String = "",
    ): GraphQLResponse? {
        val mutation = $$"""
                mutation ChannelSystemMessage($type: String!, $payload: String!) {
                    channelSystemMessage(type: $type, payload: $payload)
                }
            """.trimIndent()

        val variables = mapOf(
            "type" to type,
            "payload" to payload,
        )

        // Use channel key if peer is not paired
        val useChannelKey = peer.key.isEmpty() && channelKey.isNotEmpty()
        return if (useChannelKey) {
            execute(
                peer = peer,
                encryptionKey = channelKey,
                clientId = clientId,
                query = mutation,
                variables = variables,
                channelId = channelId,
            )
        } else {
            execute(peer = peer, clientId = clientId, query = mutation, variables = variables)
        }
    }

    /**
     * Send a chat message to a channel member.
     * Uses [channelKey] for ChaCha20 encryption and includes [channelId] via the c-cid header
     * so the receiver can look up the correct decryption key and member public keys.
     */
    suspend fun createChannelChatItem(
        peer: DPeer,
        channelId: String,
        channelKey: String,
        clientId: String,
        content: DMessageContent,
    ): GraphQLResponse? {
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

        return execute(
            peer = peer,
            encryptionKey = channelKey,
            clientId = clientId,
            query = mutation,
            variables = mapOf("content" to content.toJSONString()),
            channelId = channelId,
        )
    }

    /**
     * Core execution: signs, encrypts and sends a GraphQL request to [peer].
     *
     * @param encryptionKey  ChaCha20 key – either [DPeer.key] (peer chat) or
     *                       [DChatChannel.key] (channel chat).
     * @param channelId      When non-empty the `c-cid` header is added so the
     *                       receiver selects the channel-key path.
     */
    private suspend fun execute(
        peer: DPeer,
        encryptionKey: String = peer.key,
        clientId: String,
        query: String,
        variables: Map<String, String>,
        channelId: String = "",
    ): GraphQLResponse? {
        return try {

            val requestJson = JSONObject().apply {
                put("query", query)
                val variablesJson = JSONObject()
                variables.forEach { (key, value) ->
                    variablesJson.put(key, value)
                }
                put("variables", variablesJson)
            }.toString()

            // Generate timestamp and signature
            val timestamp = System.currentTimeMillis().toString()
            val signature = SignatureHelper.signTextAsync("$timestamp$requestJson")

            // Format: signature|timestamp|GraphQL_JSON
            val requestBody = "$signature|$timestamp|$requestJson".toRequestBody("application/json".toMediaType())

            val requestBuilder = Request.Builder()
                .url(peer.getApiUrl())
                .post(requestBody)
                .addHeader("c-id", clientId)
            if (channelId.isNotEmpty()) {
                requestBuilder.addHeader("c-cid", channelId)
            }

            val httpClient = HttpClientManager.createCryptoHttpClient(encryptionKey, 10)
            val response = httpClient.newCall(requestBuilder.build()).execute()
            val responseBody = response.body.string()

            if (response.isSuccessful) {
                LogCat.d("GraphQL response: $responseBody")
                parseGraphQLResponse(responseBody)
            } else {
                LogCat.e("GraphQL request failed: ${response.code} - $responseBody")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LogCat.e("GraphQL request error: ${e.message}")
            null
        }
    }

    private fun parseGraphQLResponse(responseBody: String): GraphQLResponse? {
        return try {
            val json = JSONObject(responseBody)
            val data = if (json.has("data") && !json.isNull("data")) {
                json.getJSONObject("data")
            } else {
                null
            }

            val errors = if (json.has("errors")) {
                val errorsArray = json.getJSONArray("errors")
                (0 until errorsArray.length()).map { i ->
                    val errorObj = errorsArray.getJSONObject(i)
                    GraphQLError(
                        message = errorObj.getString("message")
                    )
                }
            } else {
                null
            }
            GraphQLResponse(data = data, errors = errors, isSuccess = errors.isNullOrEmpty())
        } catch (e: Exception) {
            LogCat.e("Failed to parse GraphQL response: ${e.message}")
            null
        }
    }
} 