package com.ismartcoding.plain.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences

abstract class BasePreference<T> {
    abstract val default: T
    abstract val key: Preferences.Key<T>

    fun get(preferences: Preferences): T {
        return preferences[key] ?: default
    }

    suspend fun getAsync(context: Context): T {
        return context.dataStore.getAsync(key) ?: default
    }

    open suspend fun putAsync(
        context: Context,
        value: T,
    ) {
        context.dataStore.put(
            key,
            value,
        )
    }
}
