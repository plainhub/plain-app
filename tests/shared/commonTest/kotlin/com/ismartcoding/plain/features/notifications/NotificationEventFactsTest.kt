package com.ismartcoding.plain.features.notifications

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.Notification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class NotificationEventFactsTest {
    @Test
    fun publishesExistingNotificationEventNames() {
        val notification = Notification(
            id = "test",
            onlyOnce = false,
            isClearable = true,
            appId = "app.example",
            appName = "Example",
            postedAt = Instant.parse("2026-01-01T00:00:00Z"),
            silent = false,
            title = "",
            body = "",
            actions = emptyList(),
            replyActions = emptyList(),
        )
        for ((type, wire) in listOf(
            NotificationChange.NOTIFICATION_CREATED to "NOTIFICATION_CREATED",
            NotificationChange.NOTIFICATION_UPDATED to "NOTIFICATION_UPDATED",
            NotificationChange.NOTIFICATION_DELETED to "NOTIFICATION_DELETED",
        )) {
            val payload = JsonHelper.jsonEncodeToElement(NotificationEventFacts(type, notification)).jsonObject
            assertEquals(wire, payload.getValue("type").jsonPrimitive.content)
        }
    }
}
