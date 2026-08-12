package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.ChannelMember
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.enums.ChannelMemberStatus
import com.ismartcoding.plain.enums.ChatChannelStatus
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@GraphQLType
@Serializable
data class ChatChannelMember(
    val id: String,
    val status: ChannelMemberStatus,
)

@GraphQLType
@Serializable
data class ChatChannel(
    val id: String,
    val name: String,
    val owner: String,
    val members: List<ChatChannelMember>,
    val version: Long,
    val status: ChatChannelStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun ChannelMember.toModel(): ChatChannelMember {
    return ChatChannelMember(id, status)
}

fun DChatChannel.toModel(): ChatChannel {
    return ChatChannel(
        id = id,
        name = name,
        owner = owner,
        members = members.map { it.toModel() },
        version = version,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
