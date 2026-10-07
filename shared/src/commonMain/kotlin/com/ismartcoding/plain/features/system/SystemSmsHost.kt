package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.httpserver.sendMms
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.*

internal object SystemSmsHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemMmsTextFacts", "systemSmsCountFacts", "systemSmsRowsFacts", "systemSmsIdsFacts", "systemSmsConversationFacts", "systemSmsThreadFacts" -> com.ismartcoding.plain.platform.systemSmsFacts(method, params)
        "systemSendSms" -> {
            com.ismartcoding.plain.platform.sendSmsText(
                params.getValue("number").jsonPrimitive.content,
                params.getValue("body").jsonPrimitive.content,
                params["subscriptionId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.int,
                params["clientId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content,
                params["clientRequestId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content,
            )
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemTrashSms" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.trashSms(params.getValue("query").jsonPrimitive.content)
        )
        "systemRestoreSms" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.restoreSms(params.getValue("query").jsonPrimitive.content)
        )
        "systemDeleteSms" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.deleteSms(params.getValue("query").jsonPrimitive.content)
        )
        "systemSendMms" -> JsonHelper.jsonEncodeToElement(
            sendMms(
                params.getValue("number").jsonPrimitive.content,
                params.getValue("body").jsonPrimitive.content,
                params.getValue("attachmentPaths").jsonArray.map { it.jsonPrimitive.content },
                com.ismartcoding.plain.httpserver.models.ID(params.getValue("threadId").jsonPrimitive.content),
            )
        )
        "systemSimFacts" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.getSims().map { sim ->
            SimFacts(
                id = sim.id,
                label = sim.label,
                number = sim.number,
                subscriptionId = sim.subscriptionId,
            )
        })
        else -> error("Unsupported provider operation")
    }
}
