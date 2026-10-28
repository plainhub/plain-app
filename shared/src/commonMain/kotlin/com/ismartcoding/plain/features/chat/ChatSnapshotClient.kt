package com.ismartcoding.plain.features.chat

import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.ChatItem
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlApi

internal suspend fun readChatSnapshot(
    ids: List<String>,
    api: GraphqlClient = GraphqlApi,
): Map<String, ChatItem?> {
    val result = linkedMapOf<String, ChatItem?>()
    for (batch in ids.distinct().chunked(128)) {
        val selection = Query.chatSnapshot(batch)
        val data = api.execute(selection)
        batch.forEachIndexed { index, id ->
            result[id] = data.getValue("message$index")?.also {
                require(it.id == id) { "Mismatched chat snapshot identity" }
            }
        }
    }
    return result
}
