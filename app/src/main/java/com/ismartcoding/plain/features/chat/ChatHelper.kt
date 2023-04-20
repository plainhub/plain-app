package com.ismartcoding.plain.features.chat

import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.features.DeleteChatItemViewEvent
import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.helpers.FileHelper
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
        replyAsync(message)?.let {
            items.add(it)
        }
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

    private suspend fun replyAsync(input: DMessageContent): DChat? {
        if (input.value is DMessageText) {
            val commandType = ChatCommandType.parse((input.value as DMessageText).text.removePrefix(":"))
            if (commandType != null) {
                val item = DChat()
                item.content = DMessageContent(commandType.value)
                if (commandType == ChatCommandType.EXCHANGE) {
                    item.content.value = DMessageExchange()
                }
                withIO {
                    AppDatabase.instance.chatDao().insert(item)
                }
                return item
            }
        }

        return null
    }

}