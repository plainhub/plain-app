package com.ismartcoding.plain.chat

import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.ChatItemDataUpdate
import com.ismartcoding.plain.db.ChatMessageStatus
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageDeliveryResult
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageStatusData
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.helpers.JsonHelper.jsonEncode
import com.ismartcoding.plain.helpers.SearchHelper
import com.ismartcoding.plain.platform.releaseAppFile
import com.ismartcoding.plain.helpers.withIO

object ChatDbHelper {
    suspend fun insertChatItem(message: DMessageContent, fromId: String = "me", toId: String = "local", channelId: String = "", isRemote: Boolean): DChat = withIO {
        val item = DChat()
        item.fromId = fromId
        item.toId = toId
        item.channelId = channelId
        item.content = message
        item.status = if (isRemote) "pending" else "sent"
        AppDatabase.instance.chatDao().insert(item)
        item
    }

    suspend fun getChatItem(id: String): DChat? = withIO {
        AppDatabase.instance.chatDao().getById(id)
    }

    suspend fun updateChatItemStatus(item: DChat, status: String) = withIO {
        item.status = status
        AppDatabase.instance.chatDao().updateStatus(item.id, status)
    }

    suspend fun updateChatItemStatus(item: DChat, peer: DPeer, error: String?) = withIO {
        val statusData = if (error == null) {
            DMessageStatusData()
        } else {
            DMessageStatusData(listOf(DMessageDeliveryResult(peerId = peer.id, peerName = peer.name, error = error)))
        }
        item.status = statusData.aggregateStatus()
        item.statusData = if (statusData.total > 0) jsonEncode(statusData) else ""
        AppDatabase.instance.chatDao().updateStatusAndData(item.id, item.status, item.statusData)
    }

    suspend fun updateChannelChatItemStatus(item: DChat, statusData: DMessageStatusData?) = withIO {
        item.status = statusData?.aggregateStatus() ?: ChatMessageStatus.FAILED
        item.statusData = if (statusData != null && statusData.total > 0) jsonEncode(statusData) else ""
        AppDatabase.instance.chatDao().updateStatusAndData(item.id, item.status, item.statusData)
    }

    suspend fun updateChatItemContent(item: DChat, content: DMessageContent) = withIO {
        item.content = content
        AppDatabase.instance.chatDao().updateData(ChatItemDataUpdate(id = item.id, content = content))
    }

    suspend fun updateChatItemFilesContent(item: DChat, files: List<com.ismartcoding.plain.db.DMessageFile>) = withIO {
        val content = when (item.content.type) {
            DMessageType.IMAGES.value -> DMessageContent(DMessageType.IMAGES.value, DMessageImages(files))
            DMessageType.FILES.value -> DMessageContent(DMessageType.FILES.value, DMessageFiles(files))
            else -> return@withIO
        }
        updateChatItemContent(item, content)
    }

    suspend fun deleteAsync(
        id: String,
    ) = withIO {
        val chat = AppDatabase.instance.chatDao().getById(id) ?: return@withIO
        releaseFidFiles(chat.content.value)
        AppDatabase.instance.chatDao().delete(id)
        ChatManager.refreshLatestChats()
    }

    suspend fun getIdsAsync(query: String): Set<String> = withIO {
        if (query.isEmpty()) return@withIO emptySet()
        val fields = SearchHelper.parse(query)
        val idsField = fields.firstOrNull { it.name == "ids" }
        if (idsField != null) {
            return@withIO idsField.value.split(",").filter { it.isNotBlank() }.toSet()
        }
        val dao = AppDatabase.instance.chatDao()
        val channelField = fields.firstOrNull { it.name == "channel" }
        if (channelField != null) {
            return@withIO dao.getByChannelId(channelField.value).map { it.id }.toSet()
        }
        val peerField = fields.firstOrNull { it.name == "peer" }
        if (peerField != null) {
            val peerId = if (peerField.value == "local") "local" else peerField.value
            return@withIO dao.getByPeerId(peerId).map { it.id }.toSet()
        }
        emptySet()
    }

    suspend fun deleteByIdsAsync(ids: Set<String>) = withIO {
        val dao = AppDatabase.instance.chatDao()
        ids.chunked(500).forEach { chunk ->
            val chats = ids.mapNotNull { dao.getById(it) }
            releaseChatsFiles( chats)
            dao.deleteByIds(chats.map { it.id })
        }
        ChatManager.refreshLatestChats()
    }

    suspend fun deleteAllChatsAsync(peerId: String) = withIO {
        val chatDao = AppDatabase.instance.chatDao()
        releaseChatsFiles(chatDao.getByPeerId(peerId))
        chatDao.deleteByPeerId(peerId)
        ChatManager.refreshLatestChats()
    }

    suspend fun deleteAllChannelChatsAsync(channelId: String) = withIO {
        val chatDao = AppDatabase.instance.chatDao()
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
