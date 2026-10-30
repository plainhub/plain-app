package com.ismartcoding.plain.features.chat

import com.ismartcoding.plain.ui.chat.ChatViewModel

import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.features.chat.data.ChatTargetType
import com.ismartcoding.plain.models.ChatChannel
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.features.chat.models.getMessagePreview
import com.ismartcoding.plain.platform.notifications.sendChatMessageNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ismartcoding.plain.models.ChatItem
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.peer_chat
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.i18n.LocaleHelper

object ChatMessageReceiver {

    suspend fun applyCommitted(item: ChatItem, fromPeer: Peer, fromChannel: ChatChannel?) = withIO {
        val fromChannelId = item.channelId.orEmpty()
        val fromPeerId = item.fromId
        ChatViewModel.local.onMessagesCreated(
            target = if (fromChannelId.isNotEmpty()) {
                ChatTarget(fromChannelId, ChatTargetType.CHANNEL)
            } else {
                ChatTarget(fromPeerId, ChatTargetType.PEER)
            },
            items = listOf(item),
        )
        sendNotificationIfNeeded(item, fromPeer, fromChannel)
    }

    private suspend fun sendNotificationIfNeeded(
        item: ChatItem,
        fromPeer: Peer,
        fromChannel: ChatChannel?,
    ) {
        val preview = item.getMessagePreview()
        val (targetId, targetName, messageText) = if (fromChannel == null) {
            NotificationPayload(
                targetId = "peer:${fromPeer.id}",
                targetName = fromPeer.name.ifEmpty { LocaleHelper.getString(Res.string.peer_chat) },
                messageText = preview,
            )
        } else {
            NotificationPayload(
                targetId = "channel:${fromChannel.id}",
                targetName = fromChannel.name.ifEmpty { LocaleHelper.getString(Res.string.peer_chat) },
                messageText = "${fromPeer.name}: $preview",
            )
        }
        if (ChatViewModel.local.visibleConversationId.value == targetId) return
        withContext(Dispatchers.Main.immediate) {
            sendChatMessageNotification(targetId, targetName, messageText)
        }
    }
}

private data class NotificationPayload(
    val targetId: String,
    val targetName: String,
    val messageText: String,
)
