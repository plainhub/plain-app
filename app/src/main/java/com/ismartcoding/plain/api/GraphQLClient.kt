package com.ismartcoding.plain.api

import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.helpers.PeerSignatureHelper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

data class GraphQLResponse(
    val data: org.json.JSONObject? = null,
    val errors: List<GraphQLError>? = null,
    val isSuccess: Boolean = false
)

data class GraphQLError(
    val message: String
)

object GraphQLClient {

    suspend fun createChatItem(
        httpClient: OkHttpClient,
        url: String,
        clientId: String,
        content: String
    ): GraphQLResponse? {
        return try {
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

            executeRequestWithSignature(httpClient, url, clientId, mutation, variables)
        } catch (e: Exception) {
            LogCat.e("GraphQL createChatItem failed: ${e.message}")
            null
        }
    }

    // Method for requests without signature
    private suspend fun executeRequest(
        httpClient: OkHttpClient,
        url: String,
        clientId: String,
        query: String,
        variables: Map<String, String>
    ): GraphQLResponse? {
        return try {
            val requestJson = org.json.JSONObject().apply {
                put("query", query)
                val variablesJson = org.json.JSONObject()
                variables.forEach { (key, value) ->
                    variablesJson.put(key, value)
                }
                put("variables", variablesJson)
            }.toString()

            val requestBody = requestJson.toRequestBody("application/json".toMediaType())

            val request = okhttp3.Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("c-id", clientId)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                parseGraphQLResponse(responseBody)
            } else {
                LogCat.e("GraphQL request failed: ${response.code} - $responseBody")
                null
            }
        } catch (e: Exception) {
            LogCat.e("GraphQL request error: ${e.message}")
            null
        }
    }

    // Method for requests with signature
    private suspend fun executeRequestWithSignature(
        httpClient: OkHttpClient,
        url: String,
        clientId: String,
        query: String,
        variables: Map<String, String>
    ): GraphQLResponse? {
        return try {
            val requestJson = org.json.JSONObject().apply {
                put("query", query)
                val variablesJson = org.json.JSONObject()
                variables.forEach { (key, value) ->
                    variablesJson.put(key, value)
                }
                put("variables", variablesJson)
            }.toString()

            // Generate timestamp and signature
            val timestamp = System.currentTimeMillis().toString()
            val signatureData = "$timestamp$requestJson"
            val signature = PeerSignatureHelper.signMessageForPeer(signatureData)

            // Format: timestamp|signature|GraphQL_JSON
            val finalRequestString = "$timestamp|$signature|$requestJson"

            LogCat.d("GraphQL request with timestamp and signature: $finalRequestString")

            val requestBody = finalRequestString.toRequestBody("application/json".toMediaType())

            val request = okhttp3.Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("c-id", clientId)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                LogCat.d("GraphQL response: $responseBody")
                // Parse the response manually using JsonHelper for consistency
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

    /**
     * Parse GraphQL response JSON manually
     */
    private fun parseGraphQLResponse(responseBody: String): GraphQLResponse? {
        return try {
            val json = org.json.JSONObject(responseBody)

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

            val isSuccess = errors.isNullOrEmpty()
            GraphQLResponse(data = data, errors = errors, isSuccess = isSuccess)
        } catch (e: Exception) {
            LogCat.e("Failed to parse GraphQL response: ${e.message}")
            null
        }
    }
} 