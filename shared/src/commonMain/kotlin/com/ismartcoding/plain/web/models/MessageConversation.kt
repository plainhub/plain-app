package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.features.sms.DMessageConversation
import kotlin.time.Instant

data class MessageConversation(
    val id: ID,
    val address: String,
    val snippet: String,
    val date: Instant,
    val messageCount: Int,
    val read: Boolean,
)

fun DMessageConversation.toModel(): MessageConversation {
    return MessageConversation(
        id = ID(id),
        address = address,
        snippet = snippet,
        date = date,
        messageCount = messageCount,
        read = read,
    )
}
