package com.ismartcoding.plain.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences

// Backward-compat Context extensions so existing Android call sites keep compiling.
// These delegate to the shared (Context-free) implementations.
suspend fun <T> BasePreference<T>.getAsync(context: Context): T = getAsync()

@Suppress("UNCHECKED_CAST")
suspend fun <T> BasePreference<T>.putAsync(context: Context, value: T) = putAsync(value)
