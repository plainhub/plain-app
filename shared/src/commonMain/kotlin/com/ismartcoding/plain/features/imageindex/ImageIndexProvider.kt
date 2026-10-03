package com.ismartcoding.plain.features.imageindex

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

interface ImageIndexProvider {
    suspend fun handle(method: String, params: JsonObject): JsonElement
    suspend fun disconnect()
}
