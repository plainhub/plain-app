package com.ismartcoding.plain.chat.peer.transport

import com.ismartcoding.plain.chat.peer.GraphQLError
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.lib.logcat.LogCat
import org.json.JSONObject

object GraphQLResponseParser {
    fun parse(responseBody: String): GraphQLResponse {
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
                    val errorsObj = errorsArray.getJSONObject(i)
                    GraphQLError(message = errorsObj.getString("message"))
                }
            } else {
                null
            }
            GraphQLResponse(data = data, errors = errors)
        } catch (e: Exception) {
            LogCat.e("Failed to parse GraphQL response: ${e.message}")
            GraphQLResponse(null, null, e)
        }
    }
}
