package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.events.UpdateDownloadCompleteEvent
import com.ismartcoding.plain.events.UpdateDownloadFailedEvent
import com.ismartcoding.plain.events.UpdateDownloadProgressEvent
import com.ismartcoding.plain.lib.channel.ChannelEvent

class UpdateViewModel : ViewModel() {
    var updateDialogVisible = mutableStateOf(false)
    var isDownloading = mutableStateOf(false)
    var downloadProgress = mutableIntStateOf(0)
    var downloadFailed = mutableStateOf(false)
    var isDownloadComplete = mutableStateOf(false)
    var downloadedFilePath = mutableStateOf("")

    fun showDialog() { updateDialogVisible.value = true }
    fun hideDialog() { updateDialogVisible.value = false }

    fun startDownload() {
        isDownloading.value = true
        downloadProgress.intValue = 0
        downloadFailed.value = false
        isDownloadComplete.value = false
        downloadedFilePath.value = ""
    }

    fun onDownloadProgress(progress: Int) {
        downloadProgress.intValue = progress
    }

    fun onDownloadComplete(filePath: String) {
        isDownloading.value = false
        downloadProgress.intValue = 100
        isDownloadComplete.value = true
        downloadedFilePath.value = filePath
    }

    fun onDownloadFailed() {
        isDownloading.value = false
        downloadFailed.value = true
    }

    fun cancelDownload() {
        isDownloading.value = false
        downloadProgress.intValue = 0
        isDownloadComplete.value = false
        downloadedFilePath.value = ""
    }

    fun resetDownload() {
        isDownloading.value = false
        downloadProgress.intValue = 0
        downloadFailed.value = false
        isDownloadComplete.value = false
        downloadedFilePath.value = ""
    }

    fun consumeUpdateDownloadEvent(event: ChannelEvent): Boolean {
        return when (event) {
            is UpdateDownloadProgressEvent -> {
                onDownloadProgress(event.progress)
                true
            }
            is UpdateDownloadCompleteEvent -> {
                onDownloadComplete(event.filePath)
                true
            }
            is UpdateDownloadFailedEvent -> {
                onDownloadFailed()
                true
            }
            else -> false
        }
    }
}
