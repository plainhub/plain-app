package com.ismartcoding.plain.features

import android.content.Context
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
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
        withIO {
            AppDatabase.instance.chatDao().insert(item)
        }
        return item
    }

    suspend fun sendTextMessageWithLinkPreview(context: Context, text: String): Pair<DChat, List<String>> {
        val urls = LinkPreviewHelper.extractUrls(text)
        
        // 先发送原始文本消息
        val item = DChat()
        item.isMe = true
        item.content = DMessageContent(DMessageType.TEXT.value, DMessageText(text))
        withIO {
            AppDatabase.instance.chatDao().insert(item)
        }
        
        return Pair(item, urls)
    }

    suspend fun fetchAndUpdateLinkPreviews(context: Context, chatId: String, urls: List<String>) {
        if (urls.isEmpty()) return
        
        try {
            // 并行获取所有链接的预览信息
            val linkPreviews = coroutineScope {
                urls.map { url ->
                    async { LinkPreviewHelper.fetchLinkPreview(context, url) }
                }.awaitAll()
            }
            
            // 获取当前消息
            val chat = withIO { AppDatabase.instance.chatDao().getById(chatId) }
            if (chat != null && chat.content.type == DMessageType.TEXT.value) {
                val messageText = chat.content.value as DMessageText
                val updatedMessageText = DMessageText(messageText.text, linkPreviews.filter { !it.hasError })
                val updatedContent = DMessageContent(DMessageType.TEXT.value, updatedMessageText)
                
                // 更新数据库
                withIO {
                    AppDatabase.instance.chatDao().updateData(
                        ChatItemDataUpdate(chatId, updatedContent)
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getAsync(id: String): DChat? {
        return withIO { AppDatabase.instance.chatDao().getById(id) }
    }

    suspend fun deleteAsync(
        context: Context,
        id: String,
        value: Any?,
    ) {
        withIO {
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
                // 删除链接预览图片
                value.linkPreviews.forEach { preview ->
                    preview.imageLocalPath?.let { path ->
                        LinkPreviewHelper.deletePreviewImage(context, path)
                    }
                }
            }
        }
    }
}
