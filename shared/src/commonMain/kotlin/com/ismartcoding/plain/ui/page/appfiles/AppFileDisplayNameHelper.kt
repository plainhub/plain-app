package com.ismartcoding.plain.ui.page.appfiles

import com.ismartcoding.plain.platform.getExtensionFromMimeType
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.DAppFile
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages

object AppFileDisplayNameHelper {
    fun buildNameMap(chats: List<DChat>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        chats
            .sortedByDescending { it.createdAt }
            .forEach { chat ->
                when (val value = chat.content.value) {
                    is DMessageFiles -> bindItems(value.items, map)
                    is DMessageImages -> bindItems(value.items, map)
                }
            }
        return map
    }

    fun resolveDisplayName(file: DAppFile, nameMap: Map<String, String>): String {
        val fromChat = nameMap[file.id].orEmpty().trim()
        if (fromChat.isNotEmpty()) return fromChat
        val ext = getExtensionFromMimeType(file.mimeType)
        return if (ext.isNotEmpty()) "file.$ext" else "file"
    }

    suspend fun resolveDisplayNameByPath(path: String, title: String): String = withIO {
        if (title.isNotEmpty()) return@withIO title
        val fileName = path.substringAfterLast('/').substringBeforeLast('.')
        val appFile = AppDatabase.instance.appFileDao().getById(fileName)
            ?: return@withIO fileName
        val nameMap = buildNameMap(AppDatabase.instance.chatDao().getAll())
        resolveDisplayName(appFile, nameMap)
    }

    private fun bindItems(items: List<DMessageFile>, map: MutableMap<String, String>) {
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
