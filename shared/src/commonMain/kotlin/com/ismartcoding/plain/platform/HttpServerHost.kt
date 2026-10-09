package com.ismartcoding.plain.platform

import kotlinx.serialization.json.*

internal object HttpServerHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "mainGraphqlHealth" -> JsonPrimitive(com.ismartcoding.plain.platform.getOwnPackageName())
        "mainGraphqlServerFailed" -> {
            com.ismartcoding.plain.lib.coIO {
                onRustHttpServerFailed(params.getValue("generation").jsonPrimitive.long, params.getValue("message").jsonPrimitive.content)
            }
            JsonNull
        }
        "mainGraphqlShutdown" -> {
            com.ismartcoding.plain.lib.coIO {
                kotlinx.coroutines.delay(100)
                setDesktopAccessEnabled(false)
            }
            JsonNull
        }
        else -> error("Unsupported HTTP lifecycle host operation")
    }
}
