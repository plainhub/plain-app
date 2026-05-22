package com.ismartcoding.plain.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.ismartcoding.lib.logcat.LogCat

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    corruptionHandler = ReplaceFileCorruptionHandler(
        produceNewData = {
            LogCat.e("DataStore preferences corrupted, creating new empty preferences")
            emptyPreferences()
        }
    )
)

// Backward-compat Context extensions so existing Android call sites keep compiling.
// These delegate to the shared (Context-free) implementations.
suspend fun <T> BasePreference<T>.getAsync(context: Context): T = getAsync()

@Suppress("UNCHECKED_CAST")
suspend fun <T> BasePreference<T>.putAsync(context: Context, value: T) = putAsync(value)

