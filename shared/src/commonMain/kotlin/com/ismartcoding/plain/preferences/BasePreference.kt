package com.ismartcoding.plain.preferences

import androidx.datastore.preferences.core.Preferences
import com.ismartcoding.plain.helpers.withIO

abstract class BasePreference<T> {
    abstract val default: T
    abstract val key: Preferences.Key<T>

    fun get(preferences: Preferences): T {
        return preferences[key] ?: default
    }

    suspend fun getAsync(): T = withIO {
        appDataStore.getAsync(key) ?: default
    }

    open suspend fun putAsync(value: T) = withIO {
        appDataStore.put(key, value)
    }
}
