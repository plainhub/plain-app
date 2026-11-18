package com.ismartcoding.plain.features.notifications

import kotlinx.serialization.Serializable

@Serializable
internal enum class NotificationChange {
    NOTIFICATION_CREATED,
    NOTIFICATION_UPDATED,
    NOTIFICATION_DELETED,
}
