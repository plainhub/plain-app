package com.ismartcoding.plain.ui.settings

import com.ismartcoding.plain.features.settings.AppUpdateDownloadStatus
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.features.settings.AppUpdateDownloads
import kotlinx.coroutines.launch

class UpdateViewModel : ViewModel() {
    val updateDialogVisible = mutableStateOf(false)
    val isDownloading = mutableStateOf(false)
    val downloadProgress = mutableIntStateOf(0)
    val downloadFailed = mutableStateOf(false)
    val isDownloadComplete = mutableStateOf(false)

    init {
        viewModelScope.launch {
            AppUpdateDownloads.state.collect { snapshot ->
                isDownloading.value = snapshot.status == AppUpdateDownloadStatus.DOWNLOADING
                downloadProgress.intValue = snapshot.progress
                downloadFailed.value = snapshot.status == AppUpdateDownloadStatus.FAILED
                isDownloadComplete.value = snapshot.status == AppUpdateDownloadStatus.COMPLETED
            }
        }
    }

    fun showDialog() { updateDialogVisible.value = true }
    fun hideDialog() { updateDialogVisible.value = false }
}
