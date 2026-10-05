package com.ismartcoding.plain.chat

import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.enums.ChatStatus

object ChatManager {

    suspend fun refreshLatestChats() = withIO {
        ChatCacher.load()
    }

    suspend fun getChatItem(id: String): DChat? = ChatDbHelper.getChatItem(id)

    suspend fun getIdsAsync(query: String): Set<String> = ChatDbHelper.getIdsAsync(query)

    suspend fun updateStatus(item: DChat, status: ChatStatus) {
        ChatDbHelper.updateChatItemStatus(item, status)
    }

    suspend fun createChatItem(target: ChatTarget, content: DMessageContent): DChat = withIO {
        val item = RustChatService.create(target, content)
        refreshLatestChats()
        item
    }

    suspend fun sendMessage(item: DChat, target: ChatTarget, onlinePeerIds: Set<String>) = withIO {
        ChatSender.send(item)
    }

    suspend fun resendMessage(item: DChat) = withIO {
        ChatSender.send(item)
        ChatViewModel.onMessageUpdated(item.id)
    }

    suspend fun sendToChannelMembers(item: DChat, channel: DChatChannel, peerIds: List<String>) = withIO {
        ChatSender.sendToChannelMembers(item, peerIds)
    }

    suspend fun insertFilesImmediate(target: ChatTarget, files: List<DMessageFile>, isImageVideo: Boolean): DChat = withIO {
        val item = RustChatService.createFiles(target, files, isImageVideo)
        refreshLatestChats()
        item
    }

    suspend fun updateFilesMessage(
        messageId: String,
        files: List<DMessageFile>,
        target: ChatTarget,
        onlinePeerIds: Set<String>,
    ): DChat? = withIO {
        val item = RustChatService.replaceFiles(messageId, files)
        refreshLatestChats()
        item
    }

    suspend fun deleteOne(id: String) {
        ChatDbHelper.deleteAsync(id)
    }

    suspend fun deleteByIds(ids: Set<String>) {
        ChatDbHelper.deleteByIdsAsync(ids)
    }

    suspend fun clearAllMessages(target: ChatTarget) = withIO {
        RustChatService.clear(target)
    }

}
