package com.ismartcoding.plain.data

import kotlinx.serialization.Serializable

@Serializable
data class NotificationFilterData(
    val mode: String,
    val apps: Set<String>
)
