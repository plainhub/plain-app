package com.ismartcoding.plain.models

import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Share(
    val id: String,
    val name: String,
    val password: String,
    val expiresAt: Instant?,
    val readOnly: Boolean,
    val data: List<SharePath>,
    val url: String,
) {
    val isExpired: Boolean get() = expiresAt?.let { it <= TimeHelper.now() } ?: false
    val isActive: Boolean get() = !isExpired
}

@Serializable
data class ShareInput(
    val name: String,
    val password: String,
    val expiresAt: Instant?,
    val readOnly: Boolean,
    val data: List<SharePath>,
)
