package com.ismartcoding.plain.chat

import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFile

object ChatManager {

    suspend fun refreshLatestChats() = withIO {
        ChatCacher.load()
    }

    suspend fun getChatItem(id: String): DChat? = ChatDbHelper.getChatItem(id)

    suspend fun getIdsAsync(query: String): Set<String> = ChatDbHelper.getIdsAsync(query)

    suspend fun sendContent(target: ChatTarget, content: DMessageContent): DChat = RustChatService.send(target, content)
    suspend fun sendText(target: ChatTarget, text: String): DChat = RustChatService.sendText(listOf(target), text).single()
    suspend fun forward(id: String, target: ChatTarget): DChat = RustChatService.forward(id, target)
    suspend fun retry(id: String): DChat = RustChatService.retry(id)
    suspend fun deleteQuery(query: String): Int = RustChatService.deleteQuery(query)
    suspend fun sharePicked(target: ChatTarget, uris: List<String>, images: Boolean, normalize: Boolean): Boolean =
        RustChatService.share(listOf(target), uris, null, null, images, normalize)

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
        RustChatService.delete(setOf(id))
    }

    suspend fun deleteByIds(ids: Set<String>) {
        RustChatService.delete(ids)
    }

    suspend fun clearAllMessages(target: ChatTarget) = withIO {
        RustChatService.clear(target)
    }

}
