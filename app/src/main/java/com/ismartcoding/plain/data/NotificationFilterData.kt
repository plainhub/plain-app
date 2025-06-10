package com.ismartcoding.plain.data

import kotlinx.serialization.Serializable

@Serializable
data class NotificationFilterData(
    val mode: String = "blacklist",
    val apps: Set<String> = emptySet()
)