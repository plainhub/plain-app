package com.ismartcoding.plain.features.sms

import com.ismartcoding.plain.data.IData
import kotlin.time.Instant

data class DMessage(
    override var id: String,
    val body: String,
    val address: String,
    val date: Instant,
    val serviceCenter: String,
    val read: Boolean,
    val threadId: String,
    val type: Int,
    val subscriptionId: Int,
    val isMms: Boolean = false,
    val attachments: List<DMessageAttachment> = emptyList(),
) : IData

data class DMessageAttachment(
    val path: String,
    val contentType: String,
    val name: String,
)
