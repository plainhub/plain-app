package com.ismartcoding.plain.preferences

import com.ismartcoding.plain.lib.withIO

abstract class BasePreference<T> {
    abstract val default: T
    abstract val key: PreferenceKey<T>

    fun get(preferences: PreferenceSnapshot): T {
        return preferences[key] ?: default
    }

    suspend fun getAsync(): T = withIO {
        appPreferences.snapshot[key] ?: default
    }

    open suspend fun putAsync(value: T) = withIO {
        appPreferences.put(key, value)
    }
}
