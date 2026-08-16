package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.lib.extensions.getFilenameWithoutExtensionFromPath
import com.ismartcoding.plain.lib.extensions.formatDuration
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.features.dlna.sender.DlnaTransportController
import com.ismartcoding.plain.features.dlna.sender.DlnaDeviceScanner
import com.ismartcoding.plain.data.IMedia
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class CastViewModel : ViewModel() {
    val castMode: MutableState<Boolean> = mutableStateOf(false)
    val showCastDialog: MutableState<Boolean> = mutableStateOf(false)
    val isLoading: MutableState<Boolean> = mutableStateOf(false)
    val currentDeviceName: String
        get() = CastPlayer.currentDevice?.getDeviceName() ?: ""
    val hasCurrentDevice: Boolean
        get() = CastPlayer.currentDevice != null

    internal var positionUpdateJob: Job? = null

    private fun getCastUrl(path: String): String {
        return UrlHelper.getMediaHttpUrl(path)
    }

    private fun getCastAlbumArtUrl(albumUri: String): String {
        return UrlHelper.getAlbumArtHttpUrl(albumUri)
    }

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
            CastPlayer.isPlaying.value = false

            if (CastPlayer.sid.isNotEmpty()) {
                DlnaTransportController.unsubscribeEvent(device, CastPlayer.sid)
                CastPlayer.sid = ""
            }
            CastPlayer.supportsCallback.value = false
            CastPlayer.progress.value = 0f
            CastPlayer.duration.value = 0f

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

    fun seekCast(positionSeconds: Float) {
        val device = CastPlayer.currentDevice ?: return
        val target = positionSeconds.toLong().formatDuration(alwaysShowHour = true)
        viewModelScope.launchSafe {
            DlnaTransportController.seekAVTransportAsync(device, target)
            CastPlayer.progress.value = positionSeconds
        }
    }

    fun stopCast() {
        val device = CastPlayer.currentDevice
        val sid = CastPlayer.sid
        castMode.value = false
        positionUpdateJob?.cancel()
        positionUpdateJob = null
        CastPlayer.clearItems()
        CastPlayer.currentDevice = null
        CastPlayer.sid = ""
        coIO {
            if (device != null) {
                DlnaTransportController.stopAVTransportAsync(device)
                if (sid.isNotEmpty()) {
                    DlnaTransportController.unsubscribeEvent(device, sid)
                }
            }
        }
    }

    fun castPath(path: String) {
        val device = CastPlayer.currentDevice ?: return
        viewModelScope.launchSafe {
            isLoading.value = true
            CastPlayer.setCurrentUri(path)
            try {
                val title = path.getFilenameWithoutExtensionFromPath()
                DlnaTransportController.setAVTransportURIAsync(device, getCastUrl(path), title)
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
                val mediaUrl = getCastUrl(item.path)
                val albumArtUri = if (item is DAudio) {
                    getCastAlbumArtUrl("content://media/external/audio/albumart/${item.albumId}")
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

        positionUpdateJob?.cancel()

        positionUpdateJob = viewModelScope.launchSafe {
            while (CastPlayer.currentUri.value.isNotEmpty()) {
                try {
                    if (CastPlayer.isPlaying.value) {
                        val positionInfo = DlnaTransportController.getPositionInfoAsync(device)
                        CastPlayer.updatePositionInfo(positionInfo.relTime, positionInfo.trackDuration)
                        if (!CastPlayer.supportsCallback.value) {
                            CastPlayer.supportsCallback.value = true
                        }
                    }
                } catch (e: Exception) {
                    break
                }
                delay(1000)
            }
        }
    }
}
