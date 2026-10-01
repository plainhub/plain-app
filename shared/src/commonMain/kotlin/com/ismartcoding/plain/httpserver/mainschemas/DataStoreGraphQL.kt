package com.ismartcoding.plain.httpserver.mainschemas

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.platform.prefsFilePath
import com.ismartcoding.plain.preferences.appDataStore
import com.ismartcoding.plain.preferences.getPreferencesAsync
import com.ismartcoding.plain.httpserver.models.KeyValuePair

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
    getPreferencesAsync().asMap().mapNotNull { (key, value) ->
        if (key.name.startsWith(PREF_PREFIX) && value is String) {
            KeyValuePair(key.name.removePrefix(PREF_PREFIX), value)
        } else {
            null
        }
    }.sortedBy { it.key }

@GraphQLMutation
suspend fun setPref(key: String, value: String): KeyValuePair {
    if (value.encodeToByteArray().size > 65536) throw GraphQLError("invalid_pref_value")
    appDataStore.edit { it[stringPreferencesKey(prefKey(key))] = value }
    return KeyValuePair(key, value)
}

@GraphQLMutation
suspend fun deletePref(key: String): Boolean {
    appDataStore.edit { it.remove(stringPreferencesKey(prefKey(key))) }
    return true
}

@GraphQLQuery
suspend fun dataStorePath(): String {
    return prefsFilePath()
}

@GraphQLQuery
suspend fun dataStoreEntries(): List<KeyValuePair> {
    val prefs = getPreferencesAsync()
    return prefs.asMap().map { (key, value) ->
        KeyValuePair(key.name, value.toString())
    }.sortedBy { it.key }
}

@GraphQLMutation
suspend fun deleteDataStoreEntry(key: String): Boolean {
    appDataStore.edit { prefs ->
        val target = prefs.asMap().keys.find { it.name == key }
        if (target != null) {
            prefs.remove(target)
        }
    }
    return true
}

fun SchemaBuilder.addDataStoreSchema() {
}
