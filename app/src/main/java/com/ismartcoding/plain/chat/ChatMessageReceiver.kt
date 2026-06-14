package com.ismartcoding.plain.chat

import android.annotation.SuppressLint
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.chat.download.DownloadQueue
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.getMessagePreview
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.FetchLinkPreviewsEvent
import com.ismartcoding.plain.events.HMessageCreatedEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.peer_chat
import com.ismartcoding.plain.web.models.toModel

/**
 * Handles a chat message just arrived from a remote peer: validate sender +
 * channel, persist, enqueue file downloads, trigger link-preview fetching,
 * broadcast the in-app + WebSocket events, and fire a system notification
 * when the chat is not currently active. Caller is responsible for the
 * transport-level decrypt/verify step.
 */
object ChatMessageReceiver {

    /**
     * @param fromPeerId sender's peer id (from the `c-id` header).
     * @param fromChannelId sender's channel id (from the `c-cid` header),
     *                      or empty for a 1:1 peer chat.
     * @throws IllegalStateException when the channel is unknown or left.
     * @throws Exception when the sender is unknown.
     */
    suspend fun receive(
        fromPeerId: String,
        content: DMessageContent,
        fromChannelId: String = "",
    ): DChat {
        val fromPeer = AppDatabase.instance.peerDao().getById(fromPeerId)
            ?: throw Exception("invalid peer")

        val fromChannel: DChatChannel? = if (fromChannelId.isNotEmpty()) {
            val ch = AppDatabase.instance.chatChannelDao().getById(fromChannelId)
                ?: throw IllegalStateException("Unknown channel")
            if (ch.status != DChatChannel.STATUS_JOINED) {
                throw IllegalStateException("Channel not joined")
            }
            ch
        } else null

        val item = ChatDbHelper.insertChatItem(
            message = content,
            fromId = fromPeerId,
            toId = if (fromChannelId.isEmpty()) "me" else "",
            channelId = fromChannelId,
            isRemote = false,
        )

        if (item.content.type == DMessageType.TEXT.value) {
            sendEvent(FetchLinkPreviewsEvent(item))
        }

        if (item.content.type == DMessageType.FILES.value ||
            item.content.type == DMessageType.IMAGES.value
        ) {
            val files = when (item.content.value) {
                is DMessageFiles -> (item.content.value as DMessageFiles).items
                is DMessageImages -> (item.content.value as DMessageImages).items
                else -> emptyList()
            }
            files.forEach { file ->
                DownloadQueue.addDownloadTask(
                    messageFile = file,
                    peer = fromPeer,
                    messageId = item.id,
                )
            }
        }

        sendEvent(
            HMessageCreatedEvent(
                target = if (fromChannelId.isNotEmpty()) {
                    ChatTarget(fromChannelId, ChatTargetType.CHANNEL)
                } else {
                    ChatTarget(fromPeerId, ChatTargetType.PEER)
                },
                items = arrayListOf(item),
            ),
        )
        val model = item.toModel().apply { data = getContentData() }
        sendEvent(
            WebSocketEvent(
                EventType.MESSAGE_CREATED,
                JsonHelper.jsonEncode(listOf(model)),
            ),
        )

        emitNotificationIfNeeded(item, fromPeer, fromChannel)
        return item
    }

    @SuppressLint("MissingPermission")
    private fun emitNotificationIfNeeded(
        item: DChat,
        fromPeer: DPeer,
        fromChannel: DChatChannel?,
    ) {
        if (!Permission.POST_NOTIFICATIONS.can(MainApp.instance)) return
        val preview = item.getMessagePreview()
        val (targetId, targetName, messageText) = if (fromChannel == null) {
            NotificationPayload(
                targetId = "peer:${fromPeer.id}",
                targetName = fromPeer.name.ifEmpty { LocaleHelper.getStringSync(Res.string.peer_chat) },
                messageText = preview,
            )
        } else {
            NotificationPayload(
                targetId = "channel:${fromChannel.id}",
                targetName = fromChannel.name.ifEmpty { LocaleHelper.getStringSync(Res.string.peer_chat) },
                messageText = "${fromPeer.name}: $preview",
            )
        }
        if (ChatCacheManager.activeToId == targetId) return
        NotificationHelper.sendChatMessageNotification(
            context = MainApp.instance,
            targetId = targetId,
            targetName = targetName,
            messageText = messageText,
        )
    }
}

private data class NotificationPayload(
    val targetId: String,
    val targetName: String,
    val messageText: String,
)
