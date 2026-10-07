package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.toJSONString
import com.ismartcoding.plain.enums.ChatStatus
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ChatItem(
    val id: ID,
    val fromId: String,
    val toId: String,
    val channelId: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val status: ChatStatus = ChatStatus.SENT,
    val statusData: String = "",
)

fun DChat.toModel(): ChatItem {
    return ChatItem(ID(id), fromId, toId, channelId, content.toJSONString(), createdAt, updatedAt, status = status, statusData = statusData)
}
