package com.ismartcoding.plain.db

import kotlin.time.Instant

data class DTrashedMessage(
    val messageId: String, // sms numeric id, or "mms_<id>" for MMS
    val isMms: Boolean,
    val trashedAt: Instant, // used for the 30-day retention cleanup
)
