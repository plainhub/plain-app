package com.ismartcoding.plain.chat

import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.events.FetchLinkPreviewsEvent
import com.ismartcoding.plain.events.HMessageUpdatedEvent

object ChatManager {

    suspend fun refreshLatestChats() = withIO {
        ChatCacher.load()
    }

    suspend fun getChatItem(id: String): DChat? = ChatDbHelper.getChatItem(id)

    suspend fun getIdsAsync(query: String): Set<String> = ChatDbHelper.getIdsAsync(query)

    suspend fun updateStatus(item: DChat, status: String) {
        ChatDbHelper.updateChatItemStatus(item, status)
    }

    suspend fun createChatItem(target: ChatTarget, content: DMessageContent): DChat = withIO {
        val item = ChatDbHelper.insertChatItem(
            message = content,
            fromId = "me",
            toId = if (target.type == ChatTargetType.PEER) target.toId else "",
            channelId = if (target.type == ChatTargetType.CHANNEL) target.toId else "",
            isRemote = !target.isLocal(),
        )
        if (item.content.type == DMessageType.TEXT.value) {
            sendEvent(FetchLinkPreviewsEvent(item))
        }
        refreshLatestChats()
        item
    }

    suspend fun sendMessage(item: DChat, target: ChatTarget, onlinePeerIds: Set<String>) = withIO {
        ChatSender.send(item, target, onlinePeerIds)
    }

    suspend fun resendMessage(item: DChat) = withIO {
        ChatSender.send(item, item.target(), PeerCacher.getOnlinePeerIds())
        sendEvent(HMessageUpdatedEvent(item.id))
    }

    suspend fun sendToChannelMembers(item: DChat, channel: DChatChannel, peerIds: List<String>) = withIO {
        ChatSender.sendToChannelMembers(item, channel, peerIds)
    }

    suspend fun insertFilesImmediate(target: ChatTarget, files: List<DMessageFile>, isImageVideo: Boolean): DChat = withIO {
        val content = if (isImageVideo) {
            DMessageContent(DMessageType.IMAGES.value, DMessageImages(files))
        } else {
            DMessageContent(DMessageType.FILES.value, DMessageFiles(files))
        }
        val item = ChatDbHelper.insertChatItem(
            message = content,
            fromId = "me",
            toId = if (target.type == ChatTargetType.PEER) target.toId else "",
            channelId = if (target.type == ChatTargetType.CHANNEL) target.toId else "",
            isRemote = !target.isLocal(),
        )
        refreshLatestChats()
        item
    }

    suspend fun updateFilesMessage(
        messageId: String,
        files: List<DMessageFile>,
        isImageVideo: Boolean,
        target: ChatTarget,
        onlinePeerIds: Set<String>,
    ): DChat? = withIO {
        val item = ChatDbHelper.getChatItem(messageId) ?: return@withIO null
        ChatDbHelper.updateChatItemFilesContent(item, files)
        if (target.isLocal()) {
            ChatDbHelper.updateChatItemStatus(item, "sent")
        } else {
            ChatDbHelper.updateChatItemStatus(item, "pending")
            ChatSender.send(item, target, onlinePeerIds)
        }
        item
    }

    suspend fun deleteOne(id: String) {
        ChatDbHelper.deleteAsync(id)
    }

    suspend fun deleteByIds(ids: Set<String>) {
        ChatDbHelper.deleteByIdsAsync(ids)
    }

    suspend fun clearAllMessages(target: ChatTarget) = withIO {
        if (target.type == ChatTargetType.CHANNEL) {
            ChatDbHelper.deleteAllChannelChatsAsync(target.toId)
        } else {
            ChatDbHelper.deleteAllChatsAsync(target.toId)
        }
    }

    private fun DChat.target(): ChatTarget = when {
        channelId.isNotEmpty() -> ChatTarget(channelId, ChatTargetType.CHANNEL)
        toId.isEmpty() || toId == "local" -> ChatTarget("local", ChatTargetType.PEER)
        else -> ChatTarget(toId, ChatTargetType.PEER)
    }
}
