package com.ismartcoding.plain.platform

import kotlinx.serialization.json.JsonObject

expect suspend fun handleMediaActionHost(params: JsonObject): JsonObject
