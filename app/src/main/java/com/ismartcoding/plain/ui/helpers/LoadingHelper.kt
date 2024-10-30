package com.ismartcoding.plain.ui.helpers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object LoadingHelper {
    fun ensureMinimumLoadingTime(
        viewModel: ViewModel,
        startTime: Long,
        minDisplayTimeMs: Long = 500,
        updateLoadingState: (Boolean) -> Unit
    ) {
        val loadingTime = System.currentTimeMillis() - startTime
        if (loadingTime < minDisplayTimeMs) {
            viewModel.viewModelScope.launch {
                delay(minDisplayTimeMs - loadingTime)
                updateLoadingState(false)
            }
        } else {
            updateLoadingState(false)
        }
    }
} 