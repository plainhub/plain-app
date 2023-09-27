package com.ismartcoding.lib.helpers

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object JsonHelper {
    val json =
        Json {
            encodeDefaults = true
        }

    inline fun <reified T> jsonEncode(value: T): String {
        return json.encodeToString(value)
    }
}
