package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.notificationFacts
import kotlinx.serialization.json.*

internal object SystemNotificationsHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemNotificationFacts" -> JsonHelper.jsonEncodeToElement(notificationFacts())
        "systemCancelNotifications" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.cancelNotificationFacts(
            params.getValue("ids").jsonArray.map { it.jsonPrimitive.content }.toSet()))
        "systemReplyNotification" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.replyNotification(
            params.getValue("id").jsonPrimitive.content, params.getValue("actionIndex").jsonPrimitive.int, params.getValue("text").jsonPrimitive.content))
        else -> error("Unsupported provider operation")
    }
}
