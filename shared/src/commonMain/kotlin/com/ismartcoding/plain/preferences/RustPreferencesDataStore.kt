package com.ismartcoding.plain.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

internal class RustPreferencesDataStore(path: String) : DataStore<Preferences> {
    private val mutex = Mutex()
    private val state: MutableStateFlow<Preferences>

    init {
        RustPrefsBridge.open(path)
        state = MutableStateFlow(readSnapshot())
    }

    override val data = state.asStateFlow()

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = mutex.withLock {
        val old = state.value
        val next = transform(old).toPreferences()
        val oldEntries = old.asMap().mapKeys { it.key.name }
        val nextEntries = next.asMap().mapKeys { it.key.name }
        for (key in oldEntries.keys - nextEntries.keys) {
            RustPrefsBridge.remove(key)
        }
        for ((key, value) in nextEntries) {
            if (oldEntries[key] != value) {
                RustPrefsBridge.set(key, value.toJson())
            }
        }
        val persisted = readSnapshot()
        state.value = persisted
        persisted
    }

    private fun readSnapshot(): Preferences {
        val json = Json.parseToJsonElement(RustPrefsBridge.snapshot()) as JsonObject
        val prefs = emptyPreferences().toMutablePreferences()
        for ((name, value) in json) {
            when (value) {
                is JsonArray -> {
                    if (value.all { it is JsonPrimitive && it.isString }) {
                        prefs[stringSetPreferencesKey(name)] = value.map { it.jsonPrimitive.content }.toSet()
                    } else {
                        prefs[stringPreferencesKey(name)] = value.toString()
                    }
                }
                is JsonPrimitive -> when {
                    value.isString -> prefs[stringPreferencesKey(name)] = value.content
                    value.booleanOrNull != null -> prefs[booleanPreferencesKey(name)] = value.booleanOrNull!!
                    value.intOrNull != null -> prefs[intPreferencesKey(name)] = value.intOrNull!!
                    value.floatOrNull != null -> prefs[floatPreferencesKey(name)] = value.floatOrNull!!
                    else -> prefs[stringPreferencesKey(name)] = value.toString()
                }
                else -> prefs[stringPreferencesKey(name)] = value.toString()
            }
        }
        return prefs.toPreferences()
    }

    private fun Any.toJson(): String = when (this) {
        is String -> JsonPrimitive(this).toString()
        is Boolean -> JsonPrimitive(this).toString()
        is Int -> JsonPrimitive(this).toString()
        is Float -> JsonPrimitive(this).toString()
        is Set<*> -> JsonArray(this.map { JsonPrimitive(it as String) }).toString()
        else -> error("Unsupported Rust preference type: ${this::class}")
    }
}
