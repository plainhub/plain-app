package com.ismartcoding.plain.lib

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

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

    inline fun <reified T> jsonEncodeToElement(value: T): JsonElement {
        return json.encodeToJsonElement(value)
    }

    inline fun <reified T> jsonDecodeFromElement(value: JsonElement): T {
        return json.decodeFromJsonElement(value)
    }
}
