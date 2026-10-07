package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.*

internal object SystemRecordsHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemDeleteRecords" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.deleteSystemProviderFacts(
            com.ismartcoding.plain.enums.DataType.valueOf(params.getValue("provider").jsonPrimitive.content),
            params.getValue("ids").jsonArray.map { it.jsonPrimitive.content }.toSet()))
        else -> error("Unsupported provider operation")
    }
}
