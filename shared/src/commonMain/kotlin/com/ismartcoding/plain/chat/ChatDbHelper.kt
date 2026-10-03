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
import com.ismartcoding.plain.lib.JsonHelper.jsonEncode
import com.ismartcoding.plain.platform.releaseAppFile
import com.ismartcoding.plain.lib.withIO

object ChatDbHelper {
    suspend fun insertChatItem(message: DMessageContent, fromId: String = "me", toId: String = "local", channelId: String = "", isRemote: Boolean): DChat = withIO {
        val item = DChat()
        item.fromId = fromId
        item.toId = toId
        item.channelId = channelId
        item.content = message
        item.status = if (isRemote) ChatStatus.PENDING else ChatStatus.SENT
        RustChatStore.insert(item)
        item
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
        item.status = status
        RustChatStore.updateStatus(item.id, status)
    }

    suspend fun updateChatItemStatus(item: DChat, peer: DPeer, error: String?) = withIO {
        val statusData = if (error == null) {
            DMessageStatusData()
        } else {
            DMessageStatusData(listOf(DMessageDeliveryResult(peerId = peer.id, peerName = peer.name, error = error)))
        }
        item.status = statusData.aggregateStatus()
        item.statusData = if (statusData.total > 0) jsonEncode(statusData) else ""
        RustChatStore.updateStatusAndData(item.id, item.status, item.statusData)
    }

    suspend fun updateChannelChatItemStatus(item: DChat, statusData: DMessageStatusData?) = withIO {
        item.status = statusData?.aggregateStatus() ?: ChatStatus.FAILED
        item.statusData = if (statusData != null && statusData.total > 0) jsonEncode(statusData) else ""
        RustChatStore.updateStatusAndData(item.id, item.status, item.statusData)
    }

    suspend fun updateChatItemContent(item: DChat, content: DMessageContent) = withIO {
        item.content = content
        RustChatStore.updateData(ChatItemDataUpdate(id = item.id, content = content))
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
        val chat = RustChatStore.getById(id) ?: return@withIO
        releaseFidFiles(chat.content.value)
        RustChatStore.delete(id)
        ChatManager.refreshLatestChats()
    }

    suspend fun getIdsAsync(query: String): Set<String> = withIO {
        if (query.isEmpty()) return@withIO emptySet()
        RustChatStore.getIds(query)
    }

    suspend fun deleteByIdsAsync(ids: Set<String>) = withIO {
        val dao = RustChatStore
        ids.chunked(500).forEach { chunk ->
            val chats = chunk.mapNotNull { dao.getById(it) }
            releaseChatsFiles( chats)
            dao.deleteByIds(chats.map { it.id })
        }
        ChatManager.refreshLatestChats()
    }

    suspend fun deleteAllChatsAsync(peerId: String) = withIO {
        val chatDao = RustChatStore
        releaseChatsFiles(chatDao.getByPeerId(peerId))
        chatDao.deleteByPeerId(peerId)
        ChatManager.refreshLatestChats()
    }

    suspend fun deleteAllChannelChatsAsync(channelId: String) = withIO {
        val chatDao = RustChatStore
        releaseChatsFiles(chatDao.getByChannelId(channelId))
        chatDao.deleteByChannelId(channelId)
        ChatManager.refreshLatestChats()
    }

    private suspend fun releaseFidFiles(value: Any?) {
        when (value) {
            is DMessageFiles -> value.items.forEach { if (it.isFidFile()) releaseAppFile(it.localFileId()) }
            is DMessageImages -> value.items.forEach { if (it.isFidFile()) releaseAppFile(it.localFileId()) }
        }
    }

    private suspend fun releaseChatsFiles(chats: List<DChat>) {
        for (chat in chats) releaseFidFiles(chat.content.value)
    }
}
