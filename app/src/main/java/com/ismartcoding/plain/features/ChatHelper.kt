package com.ismartcoding.plain.features

import android.content.Context
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
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
            }
        }
    }
}
