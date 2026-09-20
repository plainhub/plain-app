package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.features.sms.DMessage
import com.ismartcoding.plain.enums.SmsType
import com.ismartcoding.plain.features.sms.DMessageAttachment
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class Sms(
    val id: ID,
    val body: String,
    val address: String,
    val sentAt: Instant,
    val serviceCenter: String,
    val read: Boolean,
    val threadId: ID,
    val type: SmsType,
    val subscriptionId: Int,
    val isMms: Boolean,
    val attachments: List<SmsAttachment>,
)

@GraphQLType
data class SmsAttachment(
    val path: String,
    val contentType: String,
    val name: String,
)

fun DMessage.toModel(): Sms {
    return Sms(
        ID(id),
        body,
        address,
        date,
        serviceCenter,
        read,
        ID(threadId),
        SmsType.fromInt(type),
        subscriptionId,
        isMms,
        attachments.map { it.toModel() },
    )
}

fun DMessageAttachment.toModel(): SmsAttachment {
    return SmsAttachment(path, contentType, name)
}
