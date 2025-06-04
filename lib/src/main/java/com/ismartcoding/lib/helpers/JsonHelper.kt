package com.ismartcoding.lib.helpers

import kotlinx.serialization.json.Json

object JsonHelper {
    val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    val jsonPretty =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            prettyPrint = true
        }

    inline fun <reified T> jsonEncode(value: T, pretty: Boolean = false): String {
        return if (pretty) jsonPretty.encodeToString(value) else json.encodeToString(value)
    }

    inline fun <reified T> jsonDecode(value: String): T {
        return json.decodeFromString(value)
    }
}
