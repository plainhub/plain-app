package com.ismartcoding.plain.chat.peer

import io.ktor.http.HttpStatusCode

data class PeerChatParseResult(
    val code: HttpStatusCode,
    val content: String? = null,
    val signature: String = "",
    val timestamp: Long = 0L,
)
