package com.ismartcoding.plain.ui.files.stored

import com.ismartcoding.plain.features.chat.models.MessageFile
import com.ismartcoding.plain.features.chat.models.ChatMessage
import com.ismartcoding.plain.models.AppFile
import com.ismartcoding.plain.models.ChatItem
import com.ismartcoding.plain.features.files.stored.AppFileStore
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.media.getExtensionFromMimeType

object AppFileDisplayNameHelper {
    fun buildNameMap(chats: List<ChatItem>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        chats
            .sortedByDescending { it.createdAt }
            .forEach { chat ->
                when (val value = chat.messageContent.value) {
                    is ChatMessage -> bindItems(value.items.map { it.attachment }, map)
                }
            }
        return map
    }

    fun resolveDisplayName(file: AppFile, nameMap: Map<String, String>): String {
        val fromChat = nameMap[file.id].orEmpty().trim()
        if (fromChat.isNotEmpty()) return fromChat
        val ext = getExtensionFromMimeType(file.mimeType)
        return if (ext.isNotEmpty()) "file.$ext" else "file"
    }

    suspend fun resolveDisplayNameByPath(path: String, title: String): String = withIO {
        if (title.isNotEmpty()) return@withIO title
        val fileName = path.substringAfterLast('/').substringBeforeLast('.')
        AppFileStore.displayName(fileName).ifEmpty { fileName }
    }

    private fun bindItems(items: List<MessageFile>, map: MutableMap<String, String>) {
        items.forEach { item ->
            if (item.isFidFile() && item.fileName.isNotEmpty()) {
                val hash = item.localFileId().substringBefore(".")
                if (!map.containsKey(hash)) {
                    map[hash] = item.fileName
                }
            }
        }
    }
}
