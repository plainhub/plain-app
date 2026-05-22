package com.ismartcoding.plain.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import kotlinx.coroutines.flow.first

suspend fun <T> DataStore<Preferences>.put(
    key: Preferences.Key<T>,
    value: T,
) {
    updateData { current ->
        current.toMutablePreferences().also { it[key] = value }
    }
}

suspend fun <T> DataStore<Preferences>.getAsync(key: Preferences.Key<T>): T? {
    return dataFlow.first()[key]
}
