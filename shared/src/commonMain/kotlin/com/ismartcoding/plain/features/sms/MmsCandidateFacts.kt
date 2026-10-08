package com.ismartcoding.plain.features.sms

import kotlinx.serialization.Serializable

@Serializable
data class MmsCandidateFacts(val id: Long, val address: String, val body: String, val threadId: String, val attachmentContentTypes: List<String>)
