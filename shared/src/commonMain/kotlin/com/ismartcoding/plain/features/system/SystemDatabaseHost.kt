package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.*

internal object SystemDatabaseHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemDbFacts" -> JsonHelper.jsonEncodeToElement(DatabaseFacts(
            path = com.ismartcoding.plain.platform.getDbPath(),
            tables = com.ismartcoding.plain.platform.getDbTableNames(),
        ))
        "systemDbRowCount" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.getDbTableRowCount(
            params.getValue("table").jsonPrimitive.content))
        "systemDbRows" -> JsonHelper.jsonEncodeToElement(DatabaseRowsFacts(
            rows = com.ismartcoding.plain.platform.getDbTableRows(
                params.getValue("table").jsonPrimitive.content,
                params.getValue("offset").jsonPrimitive.int,
                params.getValue("limit").jsonPrimitive.int),
        ))
        "systemDbColumns" -> JsonHelper.jsonEncodeToElement(DatabaseColumnsFacts(
            columns = com.ismartcoding.plain.platform.getDbTableColumns(params.getValue("table").jsonPrimitive.content),
        ))
        "systemDbInfo" -> JsonHelper.jsonEncodeToElement(DatabaseInfoFacts(
            idKey = com.ismartcoding.plain.platform.getDbTableInfo(params.getValue("table").jsonPrimitive.content).idKey,
        ))
        "systemCreateDbRow" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.createDbTableRow(
            params.getValue("table").jsonPrimitive.content,
            params.getValue("row").jsonPrimitive.content))
        "systemDeleteDbRows" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.deleteDbTableRows(
            params.getValue("table").jsonPrimitive.content,
            params.getValue("ids").jsonArray.map { it.jsonPrimitive.content }))
        else -> error("Unsupported provider operation")
    }
}
