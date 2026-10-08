package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.chat.peer.GraphQLError
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import kotlinx.serialization.json.*

internal object RustChannelRuntime {
    suspend fun call(command: ChannelCommand): JsonObject =
        RustContentApi.postJsonOrThrow("chat/channel", JsonHelper.jsonEncodeToElement(command).jsonObject,
            longRunning = true).getValue("result").jsonObject

    fun channel(result: JsonObject) = RustChannelStore.decode(result.getValue("channel"))
    fun response(result: JsonObject): GraphQLResponse = result.getValue("response").jsonObject.let { row ->
        GraphQLResponse(
            data = row["data"]?.takeUnless { it is JsonNull }?.toString(),
            errors = row["errors"]?.takeUnless { it is JsonNull }?.jsonArray?.map {
                GraphQLError(it.jsonObject.getValue("message").jsonPrimitive.content)
            },
        )
    }
}
