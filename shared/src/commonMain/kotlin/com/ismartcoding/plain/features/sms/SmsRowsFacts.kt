package com.ismartcoding.plain.features.sms

import kotlinx.serialization.Serializable

@Serializable
internal data class SmsRowsFacts(
    val items: List<DMessage>,
    val canonicalAddress: String,
)
