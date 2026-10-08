package com.ismartcoding.plain.chat

import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.lib.withIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ChatCacher {
    private val lock = Mutex()
    val latestChatMap = MutableStateFlow<Map<String, DChat>>(emptyMap())

    fun getLatestChat(chatId: String): DChat? = latestChatMap.value[chatId]

    suspend fun load() = withIO {
        lock.withLock { latestChatMap.value = RustChatStore.getLatestConversations() }
    }
}
