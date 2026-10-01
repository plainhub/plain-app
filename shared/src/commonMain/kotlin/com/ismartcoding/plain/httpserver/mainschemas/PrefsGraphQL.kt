package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.platform.prefsFilePath
import com.ismartcoding.plain.preferences.appPreferences
import com.ismartcoding.plain.preferences.getPreferences
import com.ismartcoding.plain.httpserver.models.KeyValuePair
import com.ismartcoding.plain.preferences.stringPreferenceKey
import kotlinx.serialization.json.JsonPrimitive

private const val PREF_PREFIX = "admin."

private fun prefKey(key: String): String {
    val valid = key.isNotEmpty() && key.length <= 128 && key.all {
        it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' || it == '-' || it == '.'
    }
    if (!valid) {
        throw GraphQLError("invalid_pref_key")
    }
    return PREF_PREFIX + key
}

@GraphQLQuery
suspend fun prefs(): List<KeyValuePair> =
    getPreferences().entries.mapNotNull { (key, value) ->
        if (key.startsWith(PREF_PREFIX) && value is JsonPrimitive && value.isString) {
            KeyValuePair(key.removePrefix(PREF_PREFIX), value.content)
        } else {
            null
        }
    }.sortedBy { it.key }

@GraphQLMutation
suspend fun setPref(key: String, value: String): KeyValuePair {
    if (value.encodeToByteArray().size > 65536) throw GraphQLError("invalid_pref_value")
    appPreferences.put(stringPreferenceKey(prefKey(key)), value)
    return KeyValuePair(key, value)
}

@GraphQLMutation
suspend fun deletePref(key: String): Boolean {
    appPreferences.remove(prefKey(key))
    return true
}

@GraphQLQuery
suspend fun prefsPath(): String {
    return prefsFilePath()
}

@GraphQLQuery
suspend fun prefEntries(): List<KeyValuePair> {
    val prefs = getPreferences()
    return prefs.entries.map { (key, value) ->
        KeyValuePair(key, if (value is JsonPrimitive && value.isString) value.content else value.toString())
    }.sortedBy { it.key }
}

@GraphQLMutation
suspend fun deletePrefEntry(key: String): Boolean {
    appPreferences.remove(key)
    return true
}

fun SchemaBuilder.addPrefsSchema() {
}
