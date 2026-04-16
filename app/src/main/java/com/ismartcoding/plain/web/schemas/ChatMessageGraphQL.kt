package com.ismartcoding.plain.web.schemas

import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.JsonHelper.jsonEncode
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.chat.ChannelChatHelper
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.chat.PeerChatHelper
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageDeliveryResult
import com.ismartcoding.plain.db.DMessageStatusData
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.events.DeleteChatItemViewEvent
import com.ismartcoding.plain.events.FetchLinkPreviewsEvent
import com.ismartcoding.plain.events.HttpApiEvents
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
                val channel = AppDatabase.instance.chatChannelDao().getById(channelId)
                if (channel != null) {
                    val statusData = ChannelChatHelper.sendAsync(channel, item.content)
                    ChatDbHelper.updateStatusAndDataAsync(item.id, statusData)
                    item.status = when {
                        statusData == null -> "failed"
                        statusData.total == 0 || statusData.allDelivered -> "sent"
                        statusData.allFailed -> "failed"
                        else -> "partial"
                    }
                }
            } else if (isPeer && peer != null) {
                val error = PeerChatHelper.sendToPeerAsync(peer, item.content)
                val statusData = if (error == null) {
                    DMessageStatusData()
                } else {
                    DMessageStatusData(
                        listOf(
                            DMessageDeliveryResult(
                                peerId = peer.id,
                                peerName = peer.name,
                                error = error,
                            ),
                        ),
                    )
                }
                ChatDbHelper.updateStatusAndDataAsync(item.id, statusData)
                item.status = if (error == null) "sent" else "failed"
                item.statusData = if (error == null) "" else jsonEncode(statusData)
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
}
