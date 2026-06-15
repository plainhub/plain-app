package com.ismartcoding.plain.chat

import android.content.Context
import com.ismartcoding.lib.helpers.JsonHelper.jsonEncode
import com.ismartcoding.lib.helpers.SearchHelper
import com.ismartcoding.plain.db.AppDatabase
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
import com.ismartcoding.plain.helpers.AppFileStore

object ChatDbHelper {
    suspend fun insertChatItem(message: DMessageContent, fromId: String = "me", toId: String = "local", channelId: String = "", isRemote: Boolean): DChat {
        val item = DChat()
        item.fromId = fromId
        item.toId = toId
        item.channelId = channelId
        item.content = message
        item.status = if (isRemote) "pending" else "sent"
        AppDatabase.instance.chatDao().insert(item)
        return item
    }

    suspend fun getChatItem(id: String): DChat? {
        return AppDatabase.instance.chatDao().getById(id)
    }

    suspend fun updateChatItemStatus(item: DChat, status: String) {
        item.status = status
        AppDatabase.instance.chatDao().updateStatus(item.id, status)
    }

    suspend fun updateChatItemStatus(item: DChat, peer: DPeer, error: String?) {
        val statusData = if (error == null) {
            DMessageStatusData()
        } else {
            DMessageStatusData(listOf(DMessageDeliveryResult(peerId = peer.id, peerName = peer.name, error = error)))
        }
        item.status = statusData.aggregateStatus()
        item.statusData = if (statusData.total > 0) jsonEncode(statusData) else ""
        AppDatabase.instance.chatDao().updateStatusAndData(item.id, item.status, item.statusData)
    }

    /**
     * Persist both [status] and per-member [statusData] for a channel message.
     * Computes the status string from [statusData] when [statusData] is provided:
     * - "sent"    → all members delivered
     * - "partial" → some delivered, some failed
     * - "failed"  → all failed (or null statusData = no leader)
     */
    suspend fun updateChannelChatItemStatus(item: DChat, statusData: DMessageStatusData?) {
        item.status = statusData?.aggregateStatus() ?: ChatMessageStatus.FAILED
        item.statusData = if (statusData != null && statusData.total > 0) jsonEncode(statusData) else ""
        AppDatabase.instance.chatDao().updateStatusAndData(item.id, item.status, item.statusData)
    }

    /**
     * Persist new [content] for an existing chat item. The in-memory [item]
     * is also mutated so callers can re-emit events / update UI without
     * re-fetching. Caller owns any "message updated" WebSocket broadcast.
     */
    suspend fun updateChatItemContent(item: DChat, content: DMessageContent) {
        item.content = content
        AppDatabase.instance.chatDao().updateData(ChatItemDataUpdate(id = item.id, content = content))
    }

    /**
     * Replace the file list of a FILES/IMAGES message — the two content
     * types whose [com.ismartcoding.plain.db.DMessageFile.uri] may transition
     * from `fsid:` (remote) to `fid:` (local) after download.
     */
    suspend fun updateChatItemFilesContent(item: DChat, files: List<com.ismartcoding.plain.db.DMessageFile>) {
        val content = when (item.content.type) {
            DMessageType.IMAGES.value -> DMessageContent(DMessageType.IMAGES.value, DMessageImages(files))
            DMessageType.FILES.value -> DMessageContent(DMessageType.FILES.value, DMessageFiles(files))
            else -> return
        }
        updateChatItemContent(item, content)
    }

    suspend fun deleteAsync(
        context: Context,
        id: String,
    ) {
        val chat = AppDatabase.instance.chatDao().getById(id) ?: return
        releaseFidFiles(context, chat.content.value)
        AppDatabase.instance.chatDao().delete(id)
    }

    /**
     * Resolve a search-style query into the set of chat item ids it matches.
     * Supports:
     * - `ids:1,2,3` — exact id list
     * - `channel:xxx` — every item in the given channel
     * - `peer:xxx` / `peer:local` — every item in the given peer conversation
     * Returns an empty set when the query is unrecognized.
     */
    suspend fun getIdsAsync(query: String): Set<String> {
        if (query.isEmpty()) return emptySet()
        val fields = SearchHelper.parse(query)
        val idsField = fields.firstOrNull { it.name == "ids" }
        if (idsField != null) {
            return idsField.value.split(",").filter { it.isNotBlank() }.toSet()
        }
        val dao = AppDatabase.instance.chatDao()
        val channelField = fields.firstOrNull { it.name == "channel" }
        if (channelField != null) {
            return dao.getByChannelId(channelField.value).map { it.id }.toSet()
        }
        val peerField = fields.firstOrNull { it.name == "peer" }
        if (peerField != null) {
            val peerId = if (peerField.value == "local") "local" else peerField.value
            return dao.getByPeerId(peerId).map { it.id }.toSet()
        }
        return emptySet()
    }

    suspend fun deleteByIdsAsync(context: Context, ids: Set<String>) {
        val dao = AppDatabase.instance.chatDao()
        ids.chunked(500).forEach { chunk ->
            val chats = ids.mapNotNull { dao.getById(it) }
            releaseChatsFiles(context, chats)
            dao.deleteByIds(chats.map { it.id })
        }
    }

    suspend fun deleteAllChatsAsync(context: Context, peerId: String) {
        val chatDao = AppDatabase.instance.chatDao()
        releaseChatsFiles(context, chatDao.getByPeerId(peerId))
        chatDao.deleteByPeerId(peerId)
    }

    suspend fun deleteAllChannelChatsAsync(context: Context, channelId: String) {
        val chatDao = AppDatabase.instance.chatDao()
        releaseChatsFiles(context, chatDao.getByChannelId(channelId))
        chatDao.deleteByChannelId(channelId)
    }

    private fun releaseFidFiles(context: Context, value: Any?) {
        when (value) {
            is DMessageFiles -> value.items.forEach { if (it.isFidFile()) AppFileStore.release(context, it.localFileId()) }
            is DMessageImages -> value.items.forEach { if (it.isFidFile()) AppFileStore.release(context, it.localFileId()) }
        }
    }

    private fun releaseChatsFiles(context: Context, chats: List<DChat>) {
        for (chat in chats) releaseFidFiles(context, chat.content.value)
    }
}