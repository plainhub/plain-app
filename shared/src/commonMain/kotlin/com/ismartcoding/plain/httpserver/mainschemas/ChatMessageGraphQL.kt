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
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.httpserver.models.ChatItem
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.models.toModel
import kotlin.reflect.typeOf

@GraphQLQuery
/** Latest-first page of a conversation, returned oldest-to-newest so clients can render directly. */
suspend fun chatItems(id: ID, offset: Int? = 0, limit: Int? = 200): List<ChatItem> {
    val dao = AppDatabase.instance.chatDao()
    val target = ChatTarget.parseId(id.value)
    val items = if (target.type == ChatTargetType.CHANNEL) {
        dao.getByChannelIdPage(target.toId, limit ?: 200, offset ?: 0)
    } else {
        dao.getByPeerIdPage(target.toId, limit ?: 200, offset ?: 0)
    }
    return items.asReversed().map { it.toModel() }
}

@GraphQLQuery
suspend fun latestChatItems(): List<ChatItem> {
    return AppDatabase.instance.chatDao().getAllLatestChats().map { it.toModel() }
}

@GraphQLMutation
suspend fun sendChatItem(toId: ID, content: String): List<ChatItem> {
    val target = ChatTarget.parseId(toId.value)
    val item = ChatManager.createChatItem(target, DChat.parseContent(content))
    ChatManager.sendMessage(item, target, emptySet())
    val model = item.toModel()
    sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(model))))
    ChatViewModel.onMessagesCreated(target, listOf(item))
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
suspend fun deleteChatItems(query: String): Boolean {
    val ids = ChatManager.getIdsAsync(query)
    ChatManager.deleteByIds(ids)
    ChatViewModel.onMessagesDeleted(ids)
    sendEvent(WebSocketEvent(EventType.MESSAGE_DELETED, JsonHelper.jsonEncode(query)))
    return true
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
