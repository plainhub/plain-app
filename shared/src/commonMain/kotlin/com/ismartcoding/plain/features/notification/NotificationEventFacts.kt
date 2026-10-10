package com.ismartcoding.plain.features.notification

import com.ismartcoding.plain.data.DNotification
import kotlinx.serialization.Serializable

@Serializable
internal data class NotificationEventFacts(val type: String, val notification: DNotification)
