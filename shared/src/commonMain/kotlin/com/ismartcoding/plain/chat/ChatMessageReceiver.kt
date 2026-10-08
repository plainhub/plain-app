package com.ismartcoding.plain.chat


import com.ismartcoding.plain.platform.canShowNotifications

import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.getMessagePreview
import com.ismartcoding.plain.events.ChatMessageNotificationEvent
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.peer_chat
import com.ismartcoding.plain.lib.sendEvent

object ChatMessageReceiver {

    suspend fun applyCommitted(item: DChat, fromPeer: DPeer, fromChannel: DChatChannel?) = withIO {
        val fromChannelId = item.channelId
        val fromPeerId = item.fromId
        ChatViewModel.onMessagesCreated(
            target = if (fromChannelId.isNotEmpty()) {
                ChatTarget(fromChannelId, ChatTargetType.CHANNEL)
            } else {
                ChatTarget(fromPeerId, ChatTargetType.PEER)
            },
            items = listOf(item),
        )
        ChatManager.refreshLatestChats()
        emitNotificationIfNeeded(item, fromPeer, fromChannel)
    }

    private fun emitNotificationIfNeeded(
        item: DChat,
        fromPeer: DPeer,
        fromChannel: DChatChannel?,
    ) {
        if (!canShowNotifications()) return
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
        if (TempData.activeToId == targetId) return
        sendEvent(ChatMessageNotificationEvent(
            targetId = targetId,
            targetName = targetName,
            messageText = messageText,
        ))
    }
}

private data class NotificationPayload(
    val targetId: String,
    val targetName: String,
    val messageText: String,
)
