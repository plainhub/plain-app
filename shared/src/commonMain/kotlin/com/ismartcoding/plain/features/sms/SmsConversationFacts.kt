package com.ismartcoding.plain.features.sms

import kotlinx.serialization.Serializable

@Serializable
internal data class SmsConversationFacts(
    val items: List<DMessageConversation>,
    val snippets: Map<String, String>,
)
