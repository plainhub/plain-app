package com.ismartcoding.plain.ui.base

import kotlinx.coroutines.CancellationException

/**
 * Run [block] in a coroutine, surface failure to the user as an error toast,
 * and let coroutine cancellation propagate normally.
 *
 * `CancellationException` is always rethrown so `viewModelScope` cleanup
 * still works correctly.
 */
suspend inline fun <T> runHandling(block: () -> T): T? {
    return try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ToastManager.showErrorToast(e.message ?: "Operation failed")
        null
    }
}
