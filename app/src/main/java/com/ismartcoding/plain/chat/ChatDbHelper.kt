package com.ismartcoding.plain.chat

import android.content.Context
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageStatusData
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.helpers.AppFileStore
import com.ismartcoding.lib.helpers.JsonHelper.jsonEncode
import com.ismartcoding.plain.db.ChatMessageStatus
import com.ismartcoding.plain.db.DMessageDeliveryResult

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

    suspend fun deleteAsync(
        context: Context,
        id: String,
    ) {
        val chat = AppDatabase.instance.chatDao().getById(id) ?: return
        releaseFidFiles(context, chat.content.value)
        AppDatabase.instance.chatDao().delete(id)
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