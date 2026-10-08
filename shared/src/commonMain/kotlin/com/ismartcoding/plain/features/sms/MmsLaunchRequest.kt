package com.ismartcoding.plain.features.sms

import kotlinx.serialization.Serializable

@Serializable
internal data class MmsLaunchRequest(val number: String, val body: String, val attachments: List<DMessageAttachment>)
