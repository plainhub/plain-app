package com.ismartcoding.plain.platform

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

actual suspend fun handleImageIndexHost(method: String, params: JsonObject): JsonElement = error("Image indexing is unavailable on iOS")
actual suspend fun disconnectImageIndexHost() = Unit
