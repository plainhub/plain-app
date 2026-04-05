package com.ismartcoding.plain.ai

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Monitors MediaStore for image changes (add, delete, modify by any app).
 * Debounces rapid notifications into a single sync request.
 */
class ImageMediaObserver(
    private val onChanged: () -> Unit,
) : ContentObserver(Handler(Looper.getMainLooper())) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var pendingJob: Job? = null

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
