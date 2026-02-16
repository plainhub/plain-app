package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.features.sms.DMessage
import com.ismartcoding.plain.features.sms.DMessageAttachment
import kotlin.time.Instant

data class Message(
    val id: ID,
    val body: String,
    val address: String,
    val date: Instant,
    val serviceCenter: String,
    val read: Boolean,
    val threadId: String,
    val type: Int,
    val subscriptionId: Int,
    val isMms: Boolean,
    val attachments: List<MessageAttachment>,
)

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
        type,
        subscriptionId,
        isMms,
        attachments.map { it.toModel() },
    )
}

fun DMessageAttachment.toModel(): MessageAttachment {
    return MessageAttachment(path, contentType, name)
}
