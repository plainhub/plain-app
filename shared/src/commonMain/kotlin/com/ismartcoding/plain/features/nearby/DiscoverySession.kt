package com.ismartcoding.plain.features.nearby

import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.*

internal suspend fun discoverySession(start: suspend () -> Unit, stop: suspend () -> Unit, poll: suspend () -> Boolean) {
    try {
        while (currentCoroutineContext().isActive) {
            try {
                if (!poll()) start()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                LogCat.e("Discovery status poll failed", error)
            }
            delay(2000)
        }
    } finally {
        withContext(NonCancellable) {
            try { stop() }
            catch (error: Exception) { LogCat.e("Discovery stop failed", error) }
        }
    }
}
