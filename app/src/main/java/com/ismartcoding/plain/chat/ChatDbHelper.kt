package com.ismartcoding.plain.chat

import android.content.Context
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DLinkPreview
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageStatusData
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.features.LinkPreviewHelper
import com.ismartcoding.plain.helpers.AppFileStore
import com.ismartcoding.lib.helpers.JsonHelper.jsonEncode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

object ChatDbHelper {
    suspend fun sendAsync(message: DMessageContent, fromId: String = "me", toId: String = "local", channelId: String = "", peer: DPeer? = null, isRemote: Boolean = peer != null): DChat {
        val item = DChat()
        item.fromId = fromId
        item.toId = toId
        item.channelId = channelId
        item.content = message
        item.status = if (isRemote) "pending" else "sent"
        AppDatabase.instance.chatDao().insert(item)
        return item
    }

    suspend fun getAsync(id: String): DChat? {
        return AppDatabase.instance.chatDao().getById(id)
    }

    suspend fun updateStatusAsync(id: String, status: String) {
        AppDatabase.instance.chatDao().updateStatus(id, status)
    }

    /**
     * Persist both [status] and per-member [statusData] for a channel message.
     * Computes the status string from [statusData] when [statusData] is provided:
     * - "sent"    → all members delivered
     * - "partial" → some delivered, some failed
     * - "failed"  → all failed (or null statusData = no leader)
     */
    suspend fun updateStatusAndDataAsync(id: String, statusData: DMessageStatusData?) {
        val status = when {
            statusData == null -> "failed"
            statusData.total == 0 -> "sent"  // no recipients (empty channel)
            statusData.allDelivered -> "sent"
            statusData.allFailed -> "failed"
            else -> "partial"
        }
        val json = if (statusData != null && statusData.total > 0) jsonEncode(statusData) else ""
        AppDatabase.instance.chatDao().updateStatusAndData(id, status, json)
    }

    suspend fun fetchLinkPreviewsAsync(context: Context, urls: List<String>): List<DLinkPreview> {
        if (urls.isEmpty()) return emptyList()

        try {
            return coroutineScope {
                urls.map { url ->
                    async { LinkPreviewHelper.fetchLinkPreview(context, url) }
                }.awaitAll()
            }
        } catch (e: Exception) {
            LogCat.e(e.toString())
        }

        return emptyList()
    }

    suspend fun deleteAsync(
        context: Context,
        id: String,
        value: Any?,
    ) {
        AppDatabase.Companion.instance.chatDao().delete(id)
        // Release content-addressable file references (decrements refCount, deletes file when 0)
        when (value) {
            is DMessageFiles -> value.items.forEach { if (it.isFidFile()) AppFileStore.release(context, it.localFileId()) }
            is DMessageImages -> value.items.forEach { if (it.isFidFile()) AppFileStore.release(context, it.localFileId()) }
        }
    }

    suspend fun deleteAllChatsByPeerAsync(context: Context, peerId: String) {
        val chatDao = AppDatabase.instance.chatDao()
        val chats = chatDao.getByChatId(peerId)

        // Release file references for all chats before deleting
        for (chat in chats) {
            when (val value = chat.content.value) {
                is DMessageFiles -> value.items.forEach { if (it.isFidFile()) AppFileStore.release(context, it.localFileId()) }
                is DMessageImages -> value.items.forEach { if (it.isFidFile()) AppFileStore.release(context, it.localFileId()) }
            }
        }

        // Delete all chat records for this peer using SQL query
        chatDao.deleteByChatId(peerId)
    }

    suspend fun deleteAllChatsAsync(context: Context, toId: String) {
        val chatDao = AppDatabase.instance.chatDao()
        val chats = chatDao.getByChatId(toId)

        // Release file references for all chats before deleting
        for (chat in chats) {
            when (val value = chat.content.value) {
                is DMessageFiles -> value.items.forEach { if (it.isFidFile()) AppFileStore.release(context, it.localFileId()) }
                is DMessageImages -> value.items.forEach { if (it.isFidFile()) AppFileStore.release(context, it.localFileId()) }
            }
        }

        chatDao.deleteByChatId(toId)
    }

    suspend fun deleteAllChannelChatsAsync(context: Context, channelId: String) {
        val chatDao = AppDatabase.instance.chatDao()
        val chats = chatDao.getByChannelId(channelId)

        // Release file references for all chats before deleting
        for (chat in chats) {
            when (val value = chat.content.value) {
                is DMessageFiles -> value.items.forEach { if (it.isFidFile()) AppFileStore.release(context, it.localFileId()) }
                is DMessageImages -> value.items.forEach { if (it.isFidFile()) AppFileStore.release(context, it.localFileId()) }
            }
        }

        chatDao.deleteByChannelId(channelId)
    }
}