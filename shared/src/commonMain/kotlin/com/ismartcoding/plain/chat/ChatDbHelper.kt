package com.ismartcoding.plain.chat

import com.ismartcoding.plain.db.ChatItemDataUpdate
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.enums.ChatStatus
import com.ismartcoding.plain.db.DMessageDeliveryResult
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageStatusData
import com.ismartcoding.plain.db.MessageType
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.lib.withIO

object ChatDbHelper {
    suspend fun insertChatItem(message: DMessageContent, toId: String = "local", channelId: String = ""): DChat = withIO {
        RustChatStore.create(toId, channelId, message)
    }

    suspend fun getChatItem(id: String): DChat? = withIO {
        RustChatStore.getById(id)
    }

    suspend fun searchAsync(query: String, limit: Int, offset: Int): List<DChat> = withIO {
        if (query.isEmpty()) return@withIO emptyList()
        RustChatStore.search(query, limit, offset)
    }

    suspend fun countAsync(query: String): Int = withIO {
        if (query.isEmpty()) return@withIO 0
        RustChatStore.count(query)
    }

    suspend fun updateChatItemStatus(item: DChat, status: ChatStatus) = withIO {
        RustChatStore.updateStatus(item.id, status)
        item.status = status
    }

    suspend fun updateChatItemStatus(item: DChat, peer: DPeer, error: String?) = withIO {
        val results = if (error == null) emptyList() else listOf(DMessageDeliveryResult(peerId = peer.id, peerName = peer.name, error = error))
        applyDelivery(item, results)
    }

    suspend fun updateChannelChatItemStatus(item: DChat, statusData: DMessageStatusData?, retry: Boolean = false) = withIO {
        applyDelivery(item, statusData?.results, retry)
    }

    private suspend fun applyDelivery(item: DChat, results: List<DMessageDeliveryResult>?, retry: Boolean = false) {
        val saved = checkNotNull(RustChatStore.delivery(item.id, results, retry)) { "Chat unavailable" }
        item.status = saved.status
        item.statusData = saved.statusData
        item.updatedAt = saved.updatedAt
    }

    suspend fun updateChatItemContent(item: DChat, content: DMessageContent) = withIO {
        RustChatStore.updateData(ChatItemDataUpdate(id = item.id, content = content))
        item.content = content
    }

    suspend fun updateChatItemFilesContent(item: DChat, files: List<com.ismartcoding.plain.db.DMessageFile>) = withIO {
        val content = when (item.content.type) {
            MessageType.IMAGES -> DMessageContent(MessageType.IMAGES, DMessageImages(files))
            MessageType.FILES -> DMessageContent(MessageType.FILES, DMessageFiles(files))
            else -> return@withIO
        }
        updateChatItemContent(item, content)
    }

    suspend fun deleteAsync(
        id: String,
    ) = withIO {
        RustChatStore.delete(id)
        ChatManager.refreshLatestChats()
    }

    suspend fun getIdsAsync(query: String): Set<String> = withIO {
        if (query.isEmpty()) return@withIO emptySet()
        RustChatStore.getIds(query)
    }

    suspend fun deleteByIdsAsync(ids: Set<String>) = withIO {
        RustChatStore.deleteByIds(ids.toList())
        ChatManager.refreshLatestChats()
    }

    suspend fun deleteAllChatsAsync(peerId: String) = withIO {
        RustChatStore.deleteByPeerId(peerId)
        ChatManager.refreshLatestChats()
    }

    suspend fun deleteAllChannelChatsAsync(channelId: String) = withIO {
        RustChatStore.deleteByChannelId(channelId)
        ChatManager.refreshLatestChats()
    }

}
