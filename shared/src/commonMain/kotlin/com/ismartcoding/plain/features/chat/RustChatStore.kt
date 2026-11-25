package com.ismartcoding.plain.features.chat

import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.ChatItem
import com.ismartcoding.plain.rust.graphql.GraphqlApi

enum class ChatSortBy { NEWEST_FIRST, OLDEST_FIRST }

object RustChatStore : ChatStoreClient()

open class ChatStoreClient internal constructor(private val api: com.ismartcoding.plain.rust.graphql.GraphqlClient = GraphqlApi) {
    suspend fun getAll(): List<ChatItem> = page()

    suspend fun getByPeerId(id: String): List<ChatItem> = page(peer = id)

    suspend fun getByChannelId(id: String): List<ChatItem> = page(channel = id)

    suspend fun getLatestConversations(): Map<String, ChatItem> =
        api.execute(Query.chatConversations()).associate { it.id to it.chat }

    suspend fun search(
        text: String,
        limit: Int,
        offset: Int,
    ): List<ChatItem> = page(text = text, limit = limit, offset = offset, sortBy = ChatSortBy.NEWEST_FIRST)

    suspend fun count(text: String): Int = api.execute(Query.chatCount("text:${term(text)}"))

    suspend fun page(
        peer: String? = null,
        channel: String? = null,
        text: String = "",
        offset: Int = 0,
        limit: Int? = null,
        sortBy: ChatSortBy = ChatSortBy.OLDEST_FIRST,
    ): List<ChatItem> {
        val query = listOfNotNull(
            peer?.let { "peer:${term(it)}" },
            channel?.let { "channel:${term(it)}" },
            "text:${term(text)}",
        ).joinToString(" ")
        return api.execute(Query.chatItems(query, offset, limit ?: Int.MAX_VALUE, sortBy))
    }

    suspend fun getById(id: String): ChatItem? = api.execute(Query.chatItem(id))

    private fun term(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
