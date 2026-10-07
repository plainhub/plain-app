package com.ismartcoding.plain.features.system

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

internal fun mediaDataType(params: JsonObject) =
    com.ismartcoding.plain.enums.DataType.valueOf(params.getValue("dataType").jsonPrimitive.content)
