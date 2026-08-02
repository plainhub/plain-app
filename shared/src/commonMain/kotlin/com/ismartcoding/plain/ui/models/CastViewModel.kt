package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.lib.extensions.getFilenameWithoutExtensionFromPath
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.features.dlna.sender.DlnaTransportController
import com.ismartcoding.plain.features.dlna.sender.DlnaDeviceScanner
import com.ismartcoding.plain.data.IMedia
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class CastViewModel : ViewModel() {
    val castMode: MutableState<Boolean> = mutableStateOf(false)
    val showCastDialog: MutableState<Boolean> = mutableStateOf(false)
    val isLoading: MutableState<Boolean> = mutableStateOf(false)

    val devices = DlnaDeviceScanner.devices

    val castItems: StateFlow<List<IMedia>> = CastPlayer.items
    val currentUri: StateFlow<String> = CastPlayer.currentUri
    val isPlaying: StateFlow<Boolean> = CastPlayer.isPlaying
    val progress: StateFlow<Float> = CastPlayer.progress
    val duration: StateFlow<Float> = CastPlayer.duration
    val supportsCallback: StateFlow<Boolean> = CastPlayer.supportsCallback
    val currentDeviceName: String
        get() = CastPlayer.currentDevice?.description?.device?.friendlyName ?: ""
    val hasCurrentDevice: Boolean
        get() = CastPlayer.currentDevice != null

    internal var positionUpdateJob: Job? = null

    fun enterCastMode() {
        castMode.value = true
        showCastDialog.value = false
    }

    fun selectDevice(hostAddress: String) {
        val device = DlnaDeviceScanner.devices.value.firstOrNull { it.hostAddress == hostAddress } ?: return
        CastPlayer.currentDevice = device
    }

    fun exitCastMode() {
        castMode.value = false
        val device = CastPlayer.currentDevice ?: return
        viewModelScope.launchSafe {
            DlnaTransportController.stopAVTransportAsync(device)
            CastPlayer.isPlaying.value = false

            // 清理投屏状态
            if (CastPlayer.sid.isNotEmpty()) {
                DlnaTransportController.unsubscribeEvent(device, CastPlayer.sid)
                CastPlayer.sid = ""
            }
            CastPlayer.supportsCallback.value = false
            CastPlayer.progress.value = 0f
            CastPlayer.duration.value = 0f

            // 取消位置更新作业
            positionUpdateJob?.cancel()
            positionUpdateJob = null
        }
    }

    fun cast(path: String) = castPath(path)

    fun cast(item: IMedia) = castItem(item)

    fun playCast() {
        val device = CastPlayer.currentDevice ?: return
        viewModelScope.launchSafe {
            DlnaTransportController.playAVTransportAsync(device)
            CastPlayer.isPlaying.value = true
        }
    }

    fun pauseCast() {
        val device = CastPlayer.currentDevice ?: return
        viewModelScope.launchSafe {
            DlnaTransportController.pauseAVTransportAsync(device)
            CastPlayer.isPlaying.value = false
        }
    }

    fun castPath(path: String) {
        val device = CastPlayer.currentDevice ?: return
        viewModelScope.launchSafe {
            isLoading.value = true
            CastPlayer.setCurrentUri(path)
            try {
                val title = path.getFilenameWithoutExtensionFromPath()
                DlnaTransportController.setAVTransportURIAsync(device, UrlHelper.getMediaHttpUrl(path), title)
                DlnaTransportController.playAVTransportAsync(device)
                CastPlayer.isPlaying.value = true
                if (CastPlayer.sid.isNotEmpty()) {
                    DlnaTransportController.unsubscribeEvent(device, CastPlayer.sid)
                    CastPlayer.sid = ""
                }
                trySubscribeEvent()
            } catch (e: Exception) {
                DialogHelper.showErrorMessage(e.message ?: "Cast failed")
            } finally {
                isLoading.value = false
            }
        }
    }

    fun castItem(item: IMedia) {
        val device = CastPlayer.currentDevice ?: return
        viewModelScope.launchSafe {
            CastPlayer.setCurrentUri(item.path)
            isLoading.value = true
            val castItems = CastPlayer.items.value
            val isInQueue = castItems.any { it.path == item.path }
            if (!isInQueue) {
                CastPlayer.addItem(item)
            }
            try {
                val mediaUrl = UrlHelper.getMediaHttpUrl(item.path)
                val albumArtUri = if (item is DAudio) {
                    UrlHelper.getAlbumArtHttpUrl("content://media/external/audio/albumart/${item.albumId}")
                } else ""
                DlnaTransportController.setAVTransportURIAsync(device, mediaUrl, item.title, albumArtUri)
                DlnaTransportController.playAVTransportAsync(device)
                CastPlayer.isPlaying.value = true
                if (CastPlayer.sid.isNotEmpty()) {
                    DlnaTransportController.unsubscribeEvent(device, CastPlayer.sid)
                    CastPlayer.sid = ""
                }
                trySubscribeEvent()
            } catch (e: Exception) {
                DialogHelper.showErrorMessage(e.message ?: "Cast failed")
            } finally {
                isLoading.value = false
            }
        }
    }

    fun reorderCastItems(from: Int, to: Int) {
        CastPlayer.reorderItems(from, to)
    }

    fun clearCastItems() {
        CastPlayer.clearItems()
    }

    fun removeCastItemAt(index: Int) {
        CastPlayer.removeItemAt(index)
    }

    suspend fun trySubscribeEvent() = withIO {
        val device = CastPlayer.currentDevice ?: return@withIO
        try {
            val sid = DlnaTransportController.subscribeEvent(device, UrlHelper.getCastCallbackUrl())
            if (sid.isNotEmpty()) {
                CastPlayer.sid = sid
                CastPlayer.supportsCallback.value = true
                startPositionUpdater()
            } else {
                CastPlayer.supportsCallback.value = false
            }
        } catch (e: Exception) {
            CastPlayer.supportsCallback.value = false
        }
    }

    fun startPositionUpdater() {
        val device = CastPlayer.currentDevice ?: return
        if (!CastPlayer.supportsCallback.value) return

        positionUpdateJob?.cancel()

        positionUpdateJob = viewModelScope.launchSafe {
            while (CastPlayer.currentUri.value.isNotEmpty() && CastPlayer.supportsCallback.value) {
                try {
                    if (CastPlayer.isPlaying.value) {
                        val positionInfo = DlnaTransportController.getPositionInfoAsync(device)
                        CastPlayer.updatePositionInfo(positionInfo.relTime, positionInfo.trackDuration)
                    }
                } catch (e: Exception) {
                    break
                }
                delay(1000)
            }
        }
    }
}
