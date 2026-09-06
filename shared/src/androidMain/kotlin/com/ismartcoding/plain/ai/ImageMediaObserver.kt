package com.ismartcoding.plain.ai

import android.database.ContentObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Monitors MediaStore for image changes (add, delete, modify by any app).
 * Debounces rapid notifications into a single sync request.
 *
 * A null ContentObserver handler dispatches onChange on the calling binder
 * thread; the body only re-arms the debounce under [this] monitor. The flush
 * runs on [Dispatchers.Default] — nothing here touches the UI.
 */
class ImageMediaObserver(
    private val onChanged: () -> Unit,
) : ContentObserver(null) {
    // SupervisorJob so a failing onChanged() cancels only that flush, not the
    // observer's ability to fire again.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pendingJob: Job? = null

    @Synchronized
    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        pendingJob?.cancel()
        pendingJob = scope.launch {
            delay(DEBOUNCE_MS)
            onChanged()
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 1500L
    }
}
