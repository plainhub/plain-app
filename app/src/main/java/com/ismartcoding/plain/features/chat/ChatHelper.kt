package com.ismartcoding.plain.features.chat

import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.features.DeleteChatItemViewEvent
import com.ismartcoding.plain.db.*
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

    suspend fun getAsync(id: String): DChat? {
        return withIO { AppDatabase.instance.chatDao().getById(id) }
    }

    suspend fun deleteAsync(id: String, value: Any?) {
        withIO {
            AppDatabase.instance.chatDao().delete(id)
            if (value is DMessageFiles) {
                value.items.forEach {
                    File(it.uri).delete()
                }
            } else if (value is DMessageImages) {
                value.items.forEach {
                    File(it.uri).delete()
                }
            }
        }
        sendEvent(DeleteChatItemViewEvent(id))
    }
}