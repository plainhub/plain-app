package com.ismartcoding.plain.platform

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

expect suspend fun handleImageIndexHost(method: String, params: JsonObject): JsonElement
expect suspend fun disconnectImageIndexHost()
