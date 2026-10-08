package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal object SystemDatabaseHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when(method) {
        "systemDbPath" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.getDbPath())
        else -> error("Unsupported database platform operation")
    }
}
