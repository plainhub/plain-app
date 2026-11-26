package com.ismartcoding.plain.features.settings

import com.ismartcoding.plain.rust.native.RustNativeApi
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.ui.components.DialogHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object AppUpdateDownloads {
    private val lock = Mutex()
    private val snapshot = MutableStateFlow(AppUpdateDownload())
    val state = snapshot.asStateFlow()

    suspend fun apply(value: AppUpdateDownload) = lock.withLock {
        if (value.coreId != snapshot.value.coreId || value.revision >= snapshot.value.revision) snapshot.value = value
    }

    fun start() = runCommand { RustNativeApi.startAppUpdateDownload() }

    fun cancel() = runCommand { RustNativeApi.cancelAppUpdateDownload() }

    fun install(onVerified: (String) -> Unit) = runCommand {
        val verified = RustNativeApi.startAppUpdateDownload()
        if (verified.status == AppUpdateDownloadStatus.COMPLETED && verified.filePath.isNotEmpty()) {
            withContext(Dispatchers.Main) { onVerified(verified.filePath) }
        }
        verified
    }

    private fun runCommand(action: suspend () -> AppUpdateDownload) = coIO {
        try {
            apply(action())
        } catch (canceled: CancellationException) {
            throw canceled
        } catch (error: Exception) {
            DialogHelper.showErrorMessage(error.toString())
        }
    }
}
