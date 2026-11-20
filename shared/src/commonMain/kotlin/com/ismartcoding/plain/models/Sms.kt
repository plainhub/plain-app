package com.ismartcoding.plain.models

import com.ismartcoding.plain.IData
import com.ismartcoding.plain.features.telephony.SmsType
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Sms(
    override var id: String,
    var body: String,
    var address: String,
    var sentAt: Instant,
    var serviceCenter: String,
    var read: Boolean,
    var threadId: String,
    var type: SmsType,
    var subscriptionId: Int,
    var isMms: Boolean,
    var attachments: List<SmsAttachment>,
) : IData
