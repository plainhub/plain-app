package com.ismartcoding.plain.features

import android.content.Context
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.db.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File

object ChatHelper {
    suspend fun sendAsync(message: DMessageContent): DChat {
        val item = DChat()
        item.isMe = true
        item.content = message
        AppDatabase.instance.chatDao().insert(item)
        return item
    }

    suspend fun fetchAndUpdateLinkPreviewsAsync(context: Context, chat: DChat, urls: List<String>): List<DLinkPreview> {
        if (urls.isEmpty()) return emptyList()

        try {
            val linkPreviews = coroutineScope {
                urls.map { url ->
                    async { LinkPreviewHelper.fetchLinkPreview(context, url) }
                }.awaitAll()
            }

            val messageText = chat.content.value as DMessageText
            val updatedMessageText = DMessageText(messageText.text, linkPreviews.filter { !it.hasError })
            val updatedContent = DMessageContent(DMessageType.TEXT.value, updatedMessageText)
            AppDatabase.instance.chatDao().updateData(
                ChatItemDataUpdate(chat.id, updatedContent)
            )
            return linkPreviews
        } catch (e: Exception) {
            LogCat.e(e.toString())
        }

        return emptyList()
    }

    suspend fun getAsync(id: String): DChat? {
        return AppDatabase.instance.chatDao().getById(id)
    }

    suspend fun deleteAsync(
        context: Context,
        id: String,
        value: Any?,
    ) {
        AppDatabase.instance.chatDao().delete(id)
        if (value is DMessageFiles) {
            value.items.forEach {
                File(it.uri.getFinalPath(context)).delete()
            }
        } else if (value is DMessageImages) {
            value.items.forEach {
                File(it.uri.getFinalPath(context)).delete()
            }
        } else if (value is DMessageText) {
            value.linkPreviews.forEach { preview ->
                preview.imageLocalPath?.let { path ->
                    LinkPreviewHelper.deletePreviewImage(context, path)
                }
            }
        }
    }
}
