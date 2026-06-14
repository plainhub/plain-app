package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.chat.ChatManager
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.events.DeleteChatItemViewEvent
import com.ismartcoding.plain.events.HMessageCreatedEvent
import com.ismartcoding.plain.events.HRetryChatItemEvent
import com.ismartcoding.plain.web.models.ChatItem
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addChatMessageSchema() {
    query("chatItems") {
        resolver { id: String ->
            val dao = AppDatabase.instance.chatDao()
            val items = if (id.startsWith("channel:")) {
                dao.getByChannelId(id.removePrefix("channel:"))
            } else {
                dao.getByChatId(id.replace("peer:", ""))
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
            val item = ChatManager.sendChatItem(toId, DChat.parseContent(content))
            val isChannel = toId.startsWith("channel:")
            val isPeer = toId.startsWith("peer:")
            val peer: DPeer? = if (isPeer) AppDatabase.instance.peerDao().getById(toId.removePrefix("peer:")) else null
            if (isChannel) {
                ChatDbHelper.deliverToChannelAsync(item)
            } else if (isPeer && peer != null) {
                ChatDbHelper.deliverToPeerAsync(item, peer)
            }
            val model = item.toModel().apply { data = getContentData() }
            sendEvent(HMessageCreatedEvent(toId, arrayListOf(item)))
            arrayListOf(model)
        }
    }
    mutation("deleteChatItem") {
        resolver { id: ID ->
            val item = ChatDbHelper.getAsync(id.value)
            if (item != null) {
                ChatDbHelper.deleteAsync(MainApp.instance, item.id, item.content.value)
                sendEvent(DeleteChatItemViewEvent(item.id))
            }
            true
        }
    }
    mutation("retryChatItem") {
        resolver { id: ID ->
            val item = ChatDbHelper.getAsync(id.value) ?: return@resolver null
            ChatDbHelper.updateStatusAsync(item.id, "pending")
            item.status = "pending"
            sendEvent(HRetryChatItemEvent(item.id))
            item.toModel()
        }
    }
}
