package com.ismartcoding.lib.helpers

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object JsonHelper {
    val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    inline fun <reified T> jsonEncode(value: T): String {
        return json.encodeToString(value)
    }

    inline fun <reified T> jsonDecode(value: String): T {
        return json.decodeFromString(value)
    }
}
