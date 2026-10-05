package com.ismartcoding.plain.platform

import com.ismartcoding.plain.enums.DataType

actual suspend fun deleteSystemProviderFacts(provider: DataType, ids: Set<String>): Set<String> = emptySet()

actual suspend fun systemSmsFacts(method: String, params: kotlinx.serialization.json.JsonObject): kotlinx.serialization.json.JsonElement =
    when (method) {
        "systemMmsTextFacts" -> kotlinx.serialization.json.JsonObject(emptyMap())
        "systemSmsIdsFacts" -> kotlinx.serialization.json.JsonArray(emptyList())
        "systemSmsCountFacts" -> kotlinx.serialization.json.Json.parseToJsonElement("{\"sms\":0,\"mms\":0}")
        "systemSmsRowsFacts" -> kotlinx.serialization.json.Json.parseToJsonElement("{\"items\":[],\"canonicalAddress\":\"\"}")
        "systemSmsConversationFacts" -> kotlinx.serialization.json.Json.parseToJsonElement("{\"items\":[],\"snippets\":{}}")
        "systemSmsThreadFacts" -> kotlinx.serialization.json.JsonArray(emptyList())
        else -> error("Unsupported SMS facts")
    }

actual suspend fun mediaBucketItemFacts(dataType: DataType): List<com.ismartcoding.plain.data.DMediaBucketItemFact> = emptyList()
