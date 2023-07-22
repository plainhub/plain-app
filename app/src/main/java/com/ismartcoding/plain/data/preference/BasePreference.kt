package com.ismartcoding.plain.data.preference

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO

abstract class BasePreference<T> {
    abstract val default: T
    abstract val key: Preferences.Key<T>

    fun get(preferences: Preferences): T {
        return preferences[key] ?: default
    }

    fun get(context: Context): T {
        return context.dataStore.get(key) ?: default
    }

    @Deprecated("Use putAsync instead", ReplaceWith("putAsync(context, value)"))
    fun put(context: Context, value: T) {
        coIO {
            putAsync(context, value)
        }
    }

    suspend fun putAsync(context: Context, value: T) {
        context.dataStore.put(
            key,
            value
        )
    }
}
