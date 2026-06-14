package com.ismartcoding.plain.chat

import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.FetchLinkPreviewsEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.web.models.toModel

/**
 * Centralizes chat-message write operations shared by GraphQL
 * (`sendChatItem` mutation) and the local ViewModel (`sendMessage`).
 *
 * `sendChatItem` covers the steps that are identical in both call sites:
 * persisting the message, broadcasting it over WebSocket, and asking the
 * link-preview worker to scan it. Delivery (peer / channel / state-machine),
 * the local UI list update, and the `HMessageCreatedEvent` hook all stay
 * with the caller.
 */
object ChatManager {

    /**
     * @param toId encoded target id: `"channel:<id>"` for channel, `"peer:<id>"`
     *   for a peer, or any other string for the default local chat.
     */
    suspend fun sendChatItem(toId: String, content: DMessageContent): DChat {
        val isChannel = toId.startsWith("channel:")
        val channelId = if (isChannel) toId.removePrefix("channel:") else ""
        val peerId = toId.removePrefix("peer:")
        val isPeer = toId.startsWith("peer:")
        val peer = if (isPeer) AppDatabase.instance.peerDao().getById(peerId) else null

        val item = ChatDbHelper.sendAsync(
            message = content,
            fromId = "me",
            toId = when {
                isChannel -> ""
                isPeer -> peerId
                else -> toId
            },
            channelId = channelId,
            peer = peer,
            isRemote = peer != null,
        )

        val model = item.toModel().apply { data = getContentData() }
        sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(model))))
        if (item.content.type == DMessageType.TEXT.value) {
            sendEvent(FetchLinkPreviewsEvent(item))
        }
        return item
    }
}
