package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.features.sms.DMessage
import com.ismartcoding.plain.enums.SmsType
import com.ismartcoding.plain.features.sms.DMessageAttachment
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class Message(
    val id: ID,
    val body: String,
    val address: String,
    val date: Instant,
    val serviceCenter: String,
    val read: Boolean,
    val threadId: String,
    val type: SmsType,
    val subscriptionId: Int,
    val isMms: Boolean,
    val attachments: List<MessageAttachment>,
)

@GraphQLType
data class MessageAttachment(
    val path: String,
    val contentType: String,
    val name: String,
)

fun DMessage.toModel(): Message {
    return Message(
        ID(id),
        body,
        address,
        date,
        serviceCenter,
        read,
        threadId,
        SmsType.fromInt(type),
        subscriptionId,
        isMms,
        attachments.map { it.toModel() },
    )
}

fun DMessageAttachment.toModel(): MessageAttachment {
    return MessageAttachment(path, contentType, name)
}
