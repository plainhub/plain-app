package com.ismartcoding.plain.platform

import kotlinx.serialization.json.JsonObject

actual suspend fun handleMediaActionHost(params: JsonObject): JsonObject = error("System media actions are unavailable on iOS")
