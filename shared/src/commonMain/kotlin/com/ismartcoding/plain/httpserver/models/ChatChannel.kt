package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.ChannelMember
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.enums.ChannelMemberStatus
import com.ismartcoding.plain.enums.ChatChannelStatus
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ChatChannelMember(
    val peerId: String,
    val status: ChannelMemberStatus,
)

@Serializable
data class ChatChannel(
    val id: String,
    val name: String,
    val ownerId: String,
    val members: List<ChatChannelMember>,
    val version: Long,
    val status: ChatChannelStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun ChannelMember.toModel(): ChatChannelMember {
    return ChatChannelMember(peerId = peerId, status = status)
}

fun DChatChannel.toModel(): ChatChannel {
    return ChatChannel(
        id = id,
        name = name,
        ownerId = ownerId,
        members = members.map { it.toModel() },
        version = version,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
