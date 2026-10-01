package com.ismartcoding.plain.preferences

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

class PreferenceKey<T>(val name: String, val decode: (JsonElement) -> T?, val encode: (T) -> JsonElement)

fun stringPreferenceKey(name: String) = PreferenceKey(name, { (it as? JsonPrimitive)?.takeIf { value -> value.isString }?.content }, ::JsonPrimitive)
fun booleanPreferenceKey(name: String) = PreferenceKey(name, { (it as? JsonPrimitive)?.booleanOrNull }, ::JsonPrimitive)
fun intPreferenceKey(name: String) = PreferenceKey(name, { (it as? JsonPrimitive)?.intOrNull }, ::JsonPrimitive)
fun floatPreferenceKey(name: String) = PreferenceKey(name, { (it as? JsonPrimitive)?.floatOrNull }, ::JsonPrimitive)
fun stringSetPreferenceKey(name: String) = PreferenceKey(
    name,
    { (it as? JsonArray)?.takeIf { array -> array.all { item -> item is JsonPrimitive && item.isString } }?.map { item -> item.jsonPrimitive.content }?.toSet() },
    { values: Set<String> -> JsonArray(values.map(::JsonPrimitive)) },
)

class PreferenceSnapshot internal constructor(val entries: Map<String, JsonElement>) {
    operator fun <T> get(key: PreferenceKey<T>): T? = entries[key.name]?.let(key.decode)
}

class RustPreferences(path: String) {
    private val mutex = Mutex()
    private val state: MutableStateFlow<PreferenceSnapshot>

    init {
        RustPrefsBridge.open(path)
        state = MutableStateFlow(readSnapshot())
    }

    val snapshots = state.asStateFlow()
    val snapshot: PreferenceSnapshot get() = state.value

    suspend fun <T> put(key: PreferenceKey<T>, value: T) = setJson(key.name, key.encode(value))

    private suspend fun setJson(key: String, value: JsonElement) = mutex.withLock {
        RustPrefsBridge.set(key, value.toString())
        state.value = readSnapshot()
    }

    suspend fun remove(key: String) = mutex.withLock {
        RustPrefsBridge.remove(key)
        state.value = readSnapshot()
    }

    private fun readSnapshot(): PreferenceSnapshot =
        PreferenceSnapshot(Json.parseToJsonElement(RustPrefsBridge.snapshot()) as JsonObject)
}

private lateinit var preferences: RustPreferences

fun initPreferences(path: String) {
    preferences = RustPreferences(path)
}

val appPreferences: RustPreferences get() = preferences

fun getPreferences(): PreferenceSnapshot = appPreferences.snapshot
