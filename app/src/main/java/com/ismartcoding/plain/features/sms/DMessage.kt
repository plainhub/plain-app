package com.ismartcoding.plain.features.sms

import com.ismartcoding.plain.data.IData
import kotlinx.datetime.Instant

data class DMessage(
    override var id: String,
    val body: String,
    val address: String,
    val date: Instant,
    val serviceCenter: String,
    val read: Boolean,
    val threadId: String,
    val type: Int,
) : IData
