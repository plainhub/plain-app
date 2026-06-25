package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.chat.ChatManager
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.events.HChatItemsDeletedEvent
import com.ismartcoding.plain.events.DeleteChatItemViewEvent
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.HMessageCreatedEvent
import com.ismartcoding.plain.events.HRetryChatItemEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.web.models.ChatItem
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addChatMessageSchema() {
    query("chatItems") {
        resolver { id: String ->
            val dao = AppDatabase.instance.chatDao()
            val target = ChatTarget.parseId(id)
            val items = if (target.type == ChatTargetType.CHANNEL) {
                dao.getByChannelId(target.toId)
            } else {
                dao.getByPeerId(target.toId)
            }
            items.map { it.toModel() }
        }
    }

    query("latestChatItems") {
        resolver { ->
            AppDatabase.instance.chatDao().getAllLatestChats().map { it.toModel() }
        }
    }
    type<ChatItem> {
        property("data") {
            resolver { c: ChatItem ->
                c.getContentData()
            }
        }
    }
    mutation("sendChatItem") {
        resolver { toId: String, content: String ->
            val target = ChatTarget.parseId(toId)
            val item = ChatManager.createChatItem(target, DChat.parseContent(content))
            ChatManager.sendMessage(item, target, emptySet())
            val model = item.toModel()
            sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(model))))
            sendEvent(HMessageCreatedEvent(target, arrayListOf(item)))
            arrayListOf(model)
        }
    }
    mutation("deleteChatItem") {
        resolver { id: ID ->
            val item = ChatManager.getChatItem(id.value)
            if (item != null) {
                ChatManager.deleteOne(item.id)
                sendEvent(DeleteChatItemViewEvent(item.id))
            }
            true
        }
    }
    mutation("deleteChatItems") {
        resolver { query: String ->
            val context = MainApp.instance
            val ids = ChatManager.getIdsAsync(query)
            ChatManager.deleteByIds(ids)
            sendEvent(HChatItemsDeletedEvent(ids))
            sendEvent(WebSocketEvent(EventType.MESSAGE_DELETED, JsonHelper.jsonEncode(query)))
            true
        }
    }
    mutation("retryChatItem") {
        resolver { id: ID ->
            val item = ChatManager.getChatItem(id.value) ?: return@resolver null
            ChatManager.updateStatus(item, "pending")
            sendEvent(HRetryChatItemEvent(item))
            item.toModel()
        }
    }
}
