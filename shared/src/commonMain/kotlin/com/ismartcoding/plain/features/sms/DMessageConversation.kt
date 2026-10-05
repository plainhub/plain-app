package com.ismartcoding.plain.features.sms

import com.ismartcoding.plain.db.IData
import kotlinx.serialization.Serializable

@Serializable
data class DMessageConversation(
    override var id: String,
    val address: String,
    val snippet: String,
    val date: kotlin.time.Instant,
    val messageCount: Int,
    val read: Boolean,
    val addresses: List<String> = if (address.isEmpty()) emptyList() else listOf(address),
) : IData
