package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.features.sms.DMessageConversation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
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
