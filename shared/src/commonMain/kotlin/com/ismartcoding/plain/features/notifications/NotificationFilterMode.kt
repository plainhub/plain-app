package com.ismartcoding.plain.features.notifications

import kotlinx.serialization.Serializable

@Serializable
enum class NotificationFilterMode {
    ALLOWLIST,
    BLACKLIST,
}
