package com.ismartcoding.plain.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.ui.base.ToastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

inline fun ViewModel.launchIO(
    crossinline onError: (Throwable) -> Unit = {
        ToastManager.showErrorToast(it.message ?: it.toString())
    },
    crossinline block: suspend CoroutineScope.() -> Unit
): Job {
    return viewModelScope.launch {
        runCatching {
            block()
        }.onFailure(onError)
    }
}