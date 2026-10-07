package com.ismartcoding.plain.platform

import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.sms.SmsConversationFacts
import com.ismartcoding.plain.features.sms.SmsCountFacts
import com.ismartcoding.plain.features.sms.SmsRowsFacts
import com.ismartcoding.plain.lib.JsonHelper

actual suspend fun deleteSystemProviderFacts(provider: DataType, ids: Set<String>): Set<String> = emptySet()

actual suspend fun systemSmsFacts(method: String, params: kotlinx.serialization.json.JsonObject): kotlinx.serialization.json.JsonElement =
    when (method) {
        "systemMmsTextFacts" -> JsonHelper.jsonEncodeToElement(emptyMap<String, String>())
        "systemSmsIdsFacts" -> JsonHelper.jsonEncodeToElement(emptyList<String>())
        "systemSmsCountFacts" -> JsonHelper.jsonEncodeToElement(
            SmsCountFacts(0, 0))
        "systemSmsRowsFacts" -> JsonHelper.jsonEncodeToElement(
            SmsRowsFacts(emptyList(), ""))
        "systemSmsConversationFacts" -> JsonHelper.jsonEncodeToElement(
            SmsConversationFacts(emptyList(), emptyMap()))
        "systemSmsThreadFacts" -> JsonHelper.jsonEncodeToElement(emptyList<Pair<String, String>>())
        else -> error("Unsupported SMS facts")
    }

actual suspend fun mediaBucketItemFacts(dataType: DataType): List<com.ismartcoding.plain.data.DMediaBucketItemFact> = emptyList()
