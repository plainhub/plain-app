package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.chat.ChatManager
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.enums.ChatStatus
import com.ismartcoding.plain.events.HChatItemsDeletedEvent
import com.ismartcoding.plain.events.DeleteChatItemViewEvent
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.HMessageCreatedEvent
import com.ismartcoding.plain.events.HRetryChatItemEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.httpserver.models.ChatItem
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.models.toModel

@GraphQLQuery
suspend fun chatItems(id: String): List<ChatItem> {
    val dao = AppDatabase.instance.chatDao()
    val target = ChatTarget.parseId(id)
    val items = if (target.type == ChatTargetType.CHANNEL) {
        dao.getByChannelId(target.toId)
    } else {
        dao.getByPeerId(target.toId)
    }
    return items.map { it.toModel() }
}

@GraphQLQuery
suspend fun latestChatItems(): List<ChatItem> {
    return AppDatabase.instance.chatDao().getAllLatestChats().map { it.toModel() }
}

@GraphQLMutation
suspend fun sendChatItem(toId: String, content: String): List<ChatItem> {
    val target = ChatTarget.parseId(toId)
    val item = ChatManager.createChatItem(target, DChat.parseContent(content))
    ChatManager.sendMessage(item, target, emptySet())
    val model = item.toModel()
    sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(model))))
    sendEvent(HMessageCreatedEvent(target, arrayListOf(item)))
    return listOf(model)
}

@GraphQLMutation
suspend fun deleteChatItem(id: ID): Boolean {
    val item = ChatManager.getChatItem(id.value)
    if (item != null) {
        ChatManager.deleteOne(item.id)
        sendEvent(DeleteChatItemViewEvent(item.id))
    }
    return true
}

@GraphQLMutation
suspend fun deleteChatItems(query: String): Boolean {
    val ids = ChatManager.getIdsAsync(query)
    ChatManager.deleteByIds(ids)
    sendEvent(HChatItemsDeletedEvent(ids))
    sendEvent(WebSocketEvent(EventType.MESSAGE_DELETED, JsonHelper.jsonEncode(query)))
    return true
}

@GraphQLMutation
suspend fun retryChatItem(id: ID): ChatItem? {
    val item = ChatManager.getChatItem(id.value) ?: return null
    ChatManager.updateStatus(item, ChatStatus.PENDING)
    sendEvent(HRetryChatItemEvent(item))
    return item.toModel()
}

fun SchemaBuilder.addChatMessageSchema() {
    type<ChatItem> {
        property("data") {
            resolver { c: ChatItem ->
                c.getContentData()
            }
        }
    }
}
