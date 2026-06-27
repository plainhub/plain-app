package com.ismartcoding.plain.chat

import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.helpers.withIO
import kotlinx.coroutines.flow.MutableStateFlow

object ChatCacher {
    val latestChatMap = MutableStateFlow<Map<String, DChat>>(emptyMap())

    fun getLatestChat(chatId: String): DChat? = latestChatMap.value[chatId]

    suspend fun load() = withIO {
        val allPeers = AppDatabase.instance.peerDao().getAll()
        val allChannels = AppDatabase.instance.chatChannelDao().getAll()
        val chatDao = AppDatabase.instance.chatDao()
        val chatCache = mutableMapOf<String, DChat>()
        val latestChats = chatDao.getAllLatestChats()
        val peerIds = allPeers.map { it.id }.toSet()
        val channelIds = allChannels.map { it.id }.toSet()

        latestChats.forEach { chat ->
            val chatId = when {
                chat.channelId.isNotEmpty() && channelIds.contains(chat.channelId) -> chat.channelId
                (chat.fromId == "me" && chat.toId == "local") || (chat.fromId == "local" && chat.toId == "me") -> "local"
                chat.fromId == "me" && peerIds.contains(chat.toId) -> chat.toId
                chat.toId == "me" && peerIds.contains(chat.fromId) -> chat.fromId
                else -> null
            }
            if (chatId != null) {
                val existing = chatCache[chatId]
                if (existing == null || chat.createdAt > existing.createdAt) chatCache[chatId] = chat
            }
        }

        latestChatMap.value = chatCache
    }
}