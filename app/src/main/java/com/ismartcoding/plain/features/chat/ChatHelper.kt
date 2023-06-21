package com.ismartcoding.plain.features.chat

import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.features.DeleteChatItemViewEvent
import com.ismartcoding.plain.db.*
import java.io.File

object ChatHelper {
    suspend fun createChatItemsAsync(message: DMessageContent): List<DChat> {
        val items = mutableListOf<DChat>()
        val item = DChat()
        item.isMe = true
        item.content = message
        withIO {
            AppDatabase.instance.chatDao().insert(item)
        }
        items.add(item)
        return items
    }

    suspend fun getAsync(id: String): DChat? {
        return withIO { AppDatabase.instance.chatDao().getById(id) }
    }

    suspend fun deleteAsync(chatItem: DChat) {
        withIO {
            AppDatabase.instance.chatDao().delete(chatItem.id)
            val v = chatItem.content.value
            if (v is DMessageFiles) {
                v.items.forEach {
                    File(it.uri).delete()
                }
            } else if (v is DMessageImages) {
                v.items.forEach {
                    File(it.uri).delete()
                }
            }
        }
        sendEvent(DeleteChatItemViewEvent(chatItem.id))
    }

}