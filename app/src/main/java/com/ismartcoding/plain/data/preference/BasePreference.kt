package com.ismartcoding.plain.data.preference

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BasePreference<T> {
    abstract val default: T
    abstract val key: Preferences.Key<T>

    fun get(preferences: Preferences): T {
        return preferences[key] ?: default
    }

    fun get(context: Context): T {
        return context.dataStore.get(key) ?: default
    }

    fun put(context: Context, scope: CoroutineScope, value: T) {
        scope.launch(Dispatchers.IO) {
            context.dataStore.put(
                key,
                value
            )
        }
    }
}