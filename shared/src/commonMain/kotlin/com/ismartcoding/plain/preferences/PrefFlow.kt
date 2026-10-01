package com.ismartcoding.plain.preferences

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement

class PrefFlow<T> internal constructor(
    internal val key: String,
    internal val isUserPref: Boolean,
    val default: T,
    private val serializer: KSerializer<T>,
    private val state: MutableStateFlow<T> = MutableStateFlow(default),
) : MutableStateFlow<T> by state {
    internal var dirty = false

    override var value: T
        get() = state.value
        set(value) = Prefs.update(this, value)

    override fun compareAndSet(expect: T, update: T): Boolean = Prefs.compareAndSet(this, expect, update)

    override suspend fun emit(value: T) { this.value = value }

    override fun tryEmit(value: T): Boolean {
        this.value = value
        return true
    }

    internal fun restore(element: JsonElement?) {
        state.value = element?.let { runCatching { Prefs.json.decodeFromJsonElement(serializer, it) }.getOrNull() } ?: default
        dirty = false
    }

    internal fun setInMemory(value: T) {
        state.value = value
        dirty = true
    }

    internal fun encode(): JsonElement = Prefs.json.encodeToJsonElement(serializer, state.value)

}
