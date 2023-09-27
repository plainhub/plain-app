package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.features.sms.DMessage
import kotlinx.datetime.Instant

data class Message(
    val id: ID,
    val body: String,
    val address: String,
    val date: Instant,
    val serviceCenter: String,
    val read: Boolean,
    val threadId: String,
    val type: Int,
)

fun DMessage.toModel(): Message {
    return Message(ID(id), body, address, date, serviceCenter, read, threadId, type)
}
