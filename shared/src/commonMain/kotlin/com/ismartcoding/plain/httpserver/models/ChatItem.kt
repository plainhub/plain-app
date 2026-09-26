package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.toJSONString
import com.ismartcoding.plain.enums.ChatStatus
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLField
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@GraphQLType
@Serializable
data class ChatItem(
    val id: ID,
    val fromId: String,
    val toId: String,
    val channelId: String,
    @GraphQLField(description = "Message envelope JSON: {type: TEXT|IMAGES|FILES|SHARE, value: {...}} — parse `value` according to `type`.")
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val status: ChatStatus = ChatStatus.SENT,
    @GraphQLField(description = "Per-recipient delivery details as JSON (peer delivery results); empty when the message has no delivery failures. Drives SENT/PARTIAL/FAILED alongside `status`.")
    val statusData: String = "",
)

fun DChat.toModel(): ChatItem {
    return ChatItem(ID(id), fromId, toId, channelId, content.toJSONString(), createdAt, updatedAt, status = status, statusData = statusData)
}

fun dchatToModel(c: DChat): ChatItem = c.toModel()
