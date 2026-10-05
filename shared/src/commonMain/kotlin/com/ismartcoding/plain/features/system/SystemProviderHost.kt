package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.extensions.parseEpochMillis
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.pinyin.Pinyin
import com.ismartcoding.plain.platform.installedPackageFacts
import com.ismartcoding.plain.platform.notificationFacts
import com.ismartcoding.plain.platform.isGranted
import kotlinx.serialization.json.*

object SystemProviderHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemPackageFacts" -> JsonArray(installedPackageFacts().map { item ->
            buildJsonObject {
                put("item", Json.parseToJsonElement(JsonHelper.jsonEncode(item)))
                put("nameSortKey", Pinyin.toPinyin(item.name).lowercase())
            }
        })
        "systemNotificationFacts" -> Json.parseToJsonElement(JsonHelper.jsonEncode(notificationFacts()))
        "systemMmsTextFacts", "systemSmsCountFacts", "systemSmsRowsFacts", "systemSmsIdsFacts", "systemSmsConversationFacts", "systemSmsThreadFacts" -> com.ismartcoding.plain.platform.systemSmsFacts(method, params)
        "systemEpochMillis" -> buildJsonObject {
            params.getValue("values").jsonArray.forEach { value ->
                val text = value.jsonPrimitive.content
                put(text, text.parseEpochMillis()?.let(::JsonPrimitive) ?: JsonNull)
            }
        }
        "systemPermissionFacts" -> buildJsonObject {
            val granted = buildJsonObject {
                params.getValue("permissions").jsonArray.forEach { item ->
                    val name = item.jsonPrimitive.content
                    put(name, com.ismartcoding.plain.platform.Permission.valueOf(name).isGranted())
                }
            }
            put("granted", granted)
        }
        "systemSendSms" -> {
            com.ismartcoding.plain.platform.sendSmsText(
                params.getValue("number").jsonPrimitive.content,
                params.getValue("body").jsonPrimitive.content,
                params["subscriptionId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.int,
                params["clientId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content,
                params["clientRequestId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content,
            )
            JsonPrimitive(true)
        }
        "systemDeleteRecords" -> JsonArray(com.ismartcoding.plain.platform.deleteSystemProviderFacts(
            com.ismartcoding.plain.enums.DataType.valueOf(params.getValue("provider").jsonPrimitive.content),
            params.getValue("ids").jsonArray.map { it.jsonPrimitive.content }.toSet()).map(::JsonPrimitive))
        "systemCancelNotifications" -> JsonArray(com.ismartcoding.plain.platform.cancelNotificationFacts(
            params.getValue("ids").jsonArray.map { it.jsonPrimitive.content }.toSet()).map(::JsonPrimitive))
        "systemReplyNotification" -> JsonPrimitive(com.ismartcoding.plain.platform.replyNotification(
            params.getValue("id").jsonPrimitive.content, params.getValue("actionIndex").jsonPrimitive.int, params.getValue("text").jsonPrimitive.content))
        "fileMetadataFacts" -> {
            val path = params.getValue("path").jsonPrimitive.content
            buildJsonObject {
                put("size", com.ismartcoding.plain.platform.statFile(path)?.size ?: 0)
                put("mimeType", com.ismartcoding.plain.platform.getContentTypeForPath(path).orEmpty())
            }
        }
        else -> error("Unsupported provider operation")
    }
}
