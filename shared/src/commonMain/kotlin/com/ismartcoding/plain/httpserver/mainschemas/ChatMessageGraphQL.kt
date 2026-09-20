package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.chat.ChatManager
import com.ismartcoding.plain.chat.ChatViewModel
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.enums.ChatStatus
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.HRetryChatItemEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.httpserver.models.ActionResult
import com.ismartcoding.plain.httpserver.models.ChatItem
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.models.toModel
import kotlin.reflect.typeOf

@GraphQLQuery(description = "Latest-first page of one conversation, returned oldest-to-newest so clients can render directly. `target` is the chat target id: a peer id, or a channel id (channel targets are prefixed, see ChatTarget).")
suspend fun chatItems(target: String, offset: Int, limit: Int, query: String): List<ChatItem> {
    val dao = AppDatabase.instance.chatDao()
    val chatTarget = ChatTarget.parseId(target)
    val text = QueryHelper.textOf(query).trim()
    val items = if (chatTarget.type == ChatTargetType.CHANNEL) {
        if (text.isEmpty()) dao.getByChannelIdPage(chatTarget.toId, limit, offset)
        else dao.getByChannelIdPageText(chatTarget.toId, "%$text%", limit, offset)
    } else {
        if (text.isEmpty()) dao.getByPeerIdPage(chatTarget.toId, limit, offset)
        else dao.getByPeerIdPageText(chatTarget.toId, "%$text%", limit, offset)
    }
    return items.asReversed().map { it.toModel() }
}

@GraphQLQuery
suspend fun latestChatItems(): List<ChatItem> {
    return AppDatabase.instance.chatDao().getAllLatestChats().map { it.toModel() }
}

@GraphQLMutation(description = "Send a chat message. `target` is the chat target id — a peer id, or a channel id (channel targets are prefixed, see ChatTarget); same value space as the chatItems query.")
suspend fun sendChatItem(target: String, content: String): List<ChatItem> {
    val chatTarget = ChatTarget.parseId(target)
    val item = ChatManager.createChatItem(chatTarget, DChat.parseContent(content))
    ChatManager.sendMessage(item, chatTarget, emptySet())
    val model = item.toModel()
    sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(model))))
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
    val ids = ChatManager.getIdsAsync(query)
    ChatManager.deleteByIds(ids)
    ChatViewModel.onMessagesDeleted(ids)
    sendEvent(WebSocketEvent(EventType.MESSAGE_DELETED, JsonHelper.jsonEncode(query)))
    return ActionResult(ids.size)
}

@GraphQLMutation
suspend fun retryChatItem(id: ID): ChatItem {
    val item = ChatManager.getChatItem(id.value)
        ?: throw GraphQLError("Chat item ${id.value} not found")
    ChatManager.updateStatus(item, ChatStatus.PENDING)
    sendEvent(HRetryChatItemEvent(item))
    return item.toModel()
}

fun SchemaBuilder.addChatMessageSchema() {
    type<ChatItem> {
        property("fromId", typeOf<ID>(), { it: ChatItem -> ID(it.fromId) })
        property("toId", typeOf<ID>(), { it: ChatItem -> ID(it.toId) })
        // channelId is "" for direct messages — expose null instead of the empty sentinel.
        property("channelId", typeOf<ID?>(), { it: ChatItem -> it.channelId.ifEmpty { null }?.let { id -> ID(id) } })
        property("data") {
            resolver { c: ChatItem ->
                c.getContentData()
            }
        }
    }
}
