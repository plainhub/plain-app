package com.ismartcoding.plain.features

import android.content.Context
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DLinkPreview
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DPeer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File

object ChatHelper {
    suspend fun sendAsync(message: DMessageContent, fromId: String = "me", toId: String = "local", peer: DPeer? = null): DChat {
        val item = DChat()
        item.fromId = fromId
        item.toId = toId
        item.content = message
        item.status = when {
            peer != null -> "pending"
            else -> "sent"
        }
        AppDatabase.instance.chatDao().insert(item)
        return item
    }

    suspend fun getAsync(id: String): DChat? {
        return AppDatabase.instance.chatDao().getById(id)
    }

    suspend fun updateStatusAsync(id: String, status: String) {
        AppDatabase.instance.chatDao().updateStatus(id, status)
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
        AppDatabase.instance.chatDao().delete(id)
//        when (value) {
//            is DMessageFiles -> {
//                value.items.forEach {
//                    File(it.uri.getFinalPath(context)).delete()
//                }
//            }
//
//            is DMessageImages -> {
//                value.items.forEach {
//                    File(it.uri.getFinalPath(context)).delete()
//                }
//            }
//
//            is DMessageText -> {
//                value.linkPreviews.forEach { preview ->
//                    preview.imageLocalPath?.let { path ->
//                        LinkPreviewHelper.deletePreviewImage(context, path)
//                    }
//                }
//            }
//        }
    }

    suspend fun deleteAllChatsByPeerAsync(context: Context, peerId: String) {
        val chatDao = AppDatabase.instance.chatDao()
        val chats = chatDao.getByChatId(peerId)
        
        // Delete all associated files first
//        for (chat in chats) {
//            when (val value = chat.content.value) {
//                is DMessageFiles -> {
//                    value.items.forEach {
//                        File(it.uri.getFinalPath(context)).delete()
//                    }
//                }
//                is DMessageImages -> {
//                    value.items.forEach {
//                        File(it.uri.getFinalPath(context)).delete()
//                    }
//                }
//                is DMessageText -> {
//                    value.linkPreviews.forEach { preview ->
//                        preview.imageLocalPath?.let { path ->
//                            LinkPreviewHelper.deletePreviewImage(context, path)
//                        }
//                    }
//                }
//            }
//        }
        
        // Delete all chat records for this peer using SQL query
        chatDao.deleteByPeerId(peerId)
    }
}
