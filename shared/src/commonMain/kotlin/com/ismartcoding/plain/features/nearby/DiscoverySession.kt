package com.ismartcoding.plain.features.nearby

import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

internal suspend fun discoverySession(start: suspend () -> Unit, stop: suspend () -> Unit, scanning: Flow<Boolean>) {
    try {
        start()
        scanning.collect { if (!it) start() }
    } finally {
        withContext(NonCancellable) {
            try { stop() }
            catch (error: Exception) { LogCat.e("Discovery stop failed", error) }
        }
    }
}
