package com.ismartcoding.plain.features.chat

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.models.ChatItem
import com.ismartcoding.plain.rust.graphql.GraphqlApi

object ChatMessageEditor {
    suspend fun updateTextAsync(item: ChatItem, newText: String): Boolean {
        val result = GraphqlApi.execute(Mutation.editChatText(item.id, newText))
        val current = result.chat
        item.fromId = current.fromId
        item.toId = current.toId
        item.channelId = current.channelId
        item.createdAt = current.createdAt
        item.status = current.status
        item.statusData = current.statusData
        item.content = current.content
        item.updatedAt = current.updatedAt
        return result.changed
    }
}
