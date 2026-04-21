package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.events.DeleteChatItemViewEvent
import com.ismartcoding.plain.events.FetchLinkPreviewsEvent
import com.ismartcoding.plain.events.HttpApiEvents
import com.ismartcoding.plain.events.RetryChatItemEvent
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addChatMessageSchema() {
    mutation("sendChatItem") {
        resolver { toId: String, content: String ->
            val isChannel = toId.startsWith("channel:")
            val channelId = if (isChannel) toId.removePrefix("channel:") else ""
            val peerId = toId.removePrefix("peer:")
            val isPeer = toId.startsWith("peer:")
            val peer: DPeer? = if (isPeer) AppDatabase.instance.peerDao().getById(peerId) else null
            val item = ChatDbHelper.sendAsync(
                DChat.parseContent(content),
                fromId = "me",
                toId = when {
                    isChannel -> ""
                    isPeer -> peerId
                    else -> toId
                },
                channelId = channelId,
                peer = peer
            )
            if (item.content.type == DMessageType.TEXT.value) {
                sendEvent(FetchLinkPreviewsEvent(item))
            }
            if (isChannel) {
                ChatDbHelper.deliverToChannelAsync(item)
            } else if (isPeer && peer != null) {
                ChatDbHelper.deliverToPeerAsync(item, peer)
            }
            sendEvent(HttpApiEvents.MessageCreatedEvent(if (isChannel) channelId else if (isPeer) peerId else toId, arrayListOf(item)))
            arrayListOf(item).map { it.toModel() }
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
            sendEvent(RetryChatItemEvent(item.id))
            item.toModel()
        }
    }
}
