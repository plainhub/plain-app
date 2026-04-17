package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class UpdateViewModel : ViewModel() {
    var updateDialogVisible = mutableStateOf(false)
    var isDownloading = mutableStateOf(false)
    var downloadProgress = mutableIntStateOf(0)
    var downloadFailed = mutableStateOf(false)

    fun showDialog() { updateDialogVisible.value = true }
    fun hideDialog() { updateDialogVisible.value = false }

    fun startDownload() {
        isDownloading.value = true
        downloadProgress.intValue = 0
        downloadFailed.value = false
    }

    fun onDownloadProgress(progress: Int) {
        downloadProgress.intValue = progress
    }

    fun onDownloadComplete() {
        isDownloading.value = false
        downloadProgress.intValue = 100
    }

    fun onDownloadFailed() {
        isDownloading.value = false
        downloadFailed.value = true
    }

    fun cancelDownload() {
        isDownloading.value = false
        downloadProgress.intValue = 0
    }

    fun resetDownload() {
        isDownloading.value = false
        downloadProgress.intValue = 0
        downloadFailed.value = false
    }
}
