package com.ismartcoding.plain.platform

import com.ismartcoding.plain.enums.DataType

expect suspend fun deleteSystemProviderFacts(provider: DataType, ids: Set<String>): Set<String>

expect suspend fun systemSmsFacts(method: String, params: kotlinx.serialization.json.JsonObject): kotlinx.serialization.json.JsonElement
