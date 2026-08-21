package com.ismartcoding.plain.features.sms

import com.ismartcoding.plain.db.IData

data class DMessageConversation(
    override var id: String,
    val address: String,
    val snippet: String,
    val date: kotlin.time.Instant,
    val messageCount: Int,
    val read: Boolean,
) : IData
