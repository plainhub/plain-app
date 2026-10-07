package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.*

internal object SystemDiscoveryHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemStopDiscovery" -> {
            com.ismartcoding.plain.discover.RustMdnsRuntime.control("stop")
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemStartDiscovery" -> {
            com.ismartcoding.plain.discover.RustMdnsRuntime.control("start")
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemDiscoveryFacts" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.discover.RustMdnsRuntime.snapshot())
        else -> error("Unsupported provider operation")
    }
}
