package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.features.sms.DMessageConversation
import kotlin.time.Instant

data class SmsConversation(
    val id: ID,
    val address: String,
    val snippet: String,
    val lastMessageAt: Instant,
    val messageCount: Int,
    val read: Boolean,
    val addresses: List<String>,
)

fun DMessageConversation.toModel(): SmsConversation {
    return SmsConversation(
        id = ID(id),
        address = address,
        snippet = snippet,
        lastMessageAt = date,
        messageCount = messageCount,
        read = read,
        addresses = addresses,
    )
}
