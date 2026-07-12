package com.ismartcoding.plain.chat.peer.transport

import com.ismartcoding.plain.chat.peer.GraphQLError
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object GraphQLResponseParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(responseBody: String): GraphQLResponse {
        return try {
            val element = json.parseToJsonElement(responseBody)
            val jsonObj = element as? JsonObject
                ?: return GraphQLResponse(null, null, IllegalStateException("Response is not a JSON object"))

            val data = if (jsonObj["data"] != null && jsonObj["data"] !is JsonNull) {
                jsonObj["data"].toString()
            } else {
                null
            }

            val errors = jsonObj["errors"]?.jsonArray?.map { err ->
                GraphQLError(
                    message = err.jsonObject["message"]?.jsonPrimitive?.content ?: "Unknown error",
                )
            }

            GraphQLResponse(data = data, errors = errors)
        } catch (e: Exception) {
            LogCat.e("Failed to parse GraphQL response: ${e.message}")
            GraphQLResponse(null, null, e)
        }
    }
}
