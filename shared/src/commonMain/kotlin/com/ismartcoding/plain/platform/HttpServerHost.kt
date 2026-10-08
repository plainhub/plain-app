package com.ismartcoding.plain.platform

import kotlinx.serialization.json.*

internal object HttpServerHost {
    suspend fun handle(method: String): JsonElement = when (method) {
        "mainGraphqlHealth" -> JsonPrimitive(com.ismartcoding.plain.platform.getOwnPackageName())
        "mainGraphqlShutdown" -> {
            com.ismartcoding.plain.lib.coIO {
                kotlinx.coroutines.delay(100)
                com.ismartcoding.plain.platform.finishHttpServerStopAsync()
            }
            JsonNull
        }
        else -> error("Unsupported HTTP lifecycle host operation")
    }
}
