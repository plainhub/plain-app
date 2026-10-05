package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.chat.RustChatStore

import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.chat.ChatManager
import com.ismartcoding.plain.chat.ChatViewModel
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.httpserver.models.ActionResult
import com.ismartcoding.plain.httpserver.models.ChatItem
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.models.toModel
import kotlin.reflect.typeOf

@GraphQLQuery(description = "Latest-first page of one conversation, returned oldest-to-newest so clients can render directly. `target` is the chat target id: a bare peer id (an optional `peer:` prefix is accepted), or a `channel:<id>` prefixed channel id.")
suspend fun chatItems(target: String, offset: Int, limit: Int, query: String): List<ChatItem> {
    return com.ismartcoding.plain.chat.RustChatService.items(target, offset, limit, query).map { it.toModel() }
}

@GraphQLQuery(description = "The most recent item of every conversation (direct and channel), newest conversation first.")
suspend fun latestChatItems(): List<ChatItem> {
    return RustChatStore.getAllLatestChats().map { it.toModel() }
}

@GraphQLMutation(description = "Send a chat message. `target` is the chat target id — a bare peer id (an optional `peer:` prefix is accepted), or a `channel:<id>` prefixed channel id; same value space as the chatItems query. `content` is the message envelope JSON ({type: TEXT|IMAGES|FILES|SHARE, value: {...}}), same shape as ChatItem.content.")
suspend fun sendChatItem(target: String, content: String): List<ChatItem> {
    val chatTarget = ChatTarget.parseId(target)
    val item = ChatManager.sendContent(chatTarget, DChat.parseContent(content))
    val model = item.toModel()
    ChatViewModel.onMessagesCreated(chatTarget, listOf(item))
    return listOf(model)
}

@GraphQLMutation
suspend fun deleteChatItem(id: ID): Boolean {
    val item = ChatManager.getChatItem(id.value)
    if (item != null) {
        ChatManager.deleteOne(item.id)
        ChatViewModel.onMessagesDeleted(setOf(item.id))
    }
    return true
}

@GraphQLMutation
suspend fun deleteChatItems(query: String): ActionResult {
    return ActionResult(ChatManager.deleteQuery(query))
}

@GraphQLMutation
suspend fun retryChatItem(id: ID): ChatItem {
    val item = ChatManager.getChatItem(id.value)
        ?: throw GraphQLError("Chat item ${id.value} not found")
    return ChatManager.retry(item.id).toModel()
}

fun SchemaBuilder.addChatMessageSchema() {
    type<ChatItem> {
        property("fromId", typeOf<ID>(), { it: ChatItem -> ID(it.fromId) }) {
            description = "`me` when this device created the item, otherwise the client id of the originating peer."
        }
        property("toId", typeOf<ID>(), { it: ChatItem -> ID(it.toId) }) {
            description = "Peer id for direct messages; empty for channel messages — `channelId` is authoritative there."
        }
        // channelId is "" for direct messages — expose null instead of the empty sentinel.
        property("channelId", typeOf<ID?>(), { it: ChatItem -> it.channelId.ifEmpty { null }?.let { id -> ID(id) } })
    }
}
