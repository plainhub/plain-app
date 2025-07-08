package com.ismartcoding.plain.chat

import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.db.DPeer
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
    suspend fun createChatItem(
        peer: DPeer,
        clientId: String,
        content: String
    ): GraphQLResponse? {
        val mutation = """
                mutation CreateChatItem(${'$'}content: String!) {
                    createChatItem(content: ${'$'}content) {
                        id
                        fromId
                        toId
                        createdAt
                    }
                }
            """.trimIndent()

        val variables = mapOf(
            "content" to content
        )

        return execute(peer, clientId, mutation, variables)
    }

    private suspend fun execute(
        peer: DPeer,
        clientId: String,
        query: String,
        variables: Map<String, String>
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

            val request = Request.Builder()
                .url(peer.getApiUrl())
                .post(requestBody)
                .addHeader("c-id", clientId)
                .build()

            val httpClient = HttpClientManager.createCryptoHttpClient(peer.key, 10)
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
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