package com.ismartcoding.plain.features.notification

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.data.DNotification
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.jsonObject

internal object RustNotificationEvents {
    suspend fun publish(type: EventType, notification: DNotification) {
        RustContentApi.postJsonOrThrow("system/notification-event", JsonHelper.jsonEncodeToElement(NotificationEventFacts(type.name, notification)).jsonObject)
    }
}
