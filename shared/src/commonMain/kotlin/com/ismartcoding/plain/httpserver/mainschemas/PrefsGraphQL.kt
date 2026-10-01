package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.preferences.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

private fun validatePrefKey(key: String) {
    if (key.isEmpty() || key.length > 128 || !key.all {
            it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' || it == '-' || it == '.'
        }) {
        throw GraphQLError("invalid_pref_key")
    }
}

@GraphQLQuery
suspend fun userPrefs(): JsonElement = JsonObject(UserPrefs.snapshot)

@GraphQLQuery
suspend fun systemPrefs(): JsonElement = JsonObject(Prefs.systemSnapshot)

@GraphQLMutation
suspend fun setUserPref(key: String, value: JsonElement): Boolean {
    validatePrefKey(key)
    if (value.toString().encodeToByteArray().size > 65536) {
        throw GraphQLError("invalid_pref_value")
    }
    UserPrefs.set(key, value)
    return true
}

@GraphQLMutation
suspend fun removeUserPref(key: String): Boolean {
    validatePrefKey(key)
    UserPrefs.remove(key)
    return true
}

fun SchemaBuilder.addPrefsSchema() {
}
