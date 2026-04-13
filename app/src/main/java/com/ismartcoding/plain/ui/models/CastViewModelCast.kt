package com.ismartcoding.plain.ui.models

import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.extensions.getFilenameWithoutExtensionFromPath
import com.ismartcoding.plain.data.DAudio
import com.ismartcoding.plain.features.dlna.sender.DlnaTransportController
import com.ismartcoding.plain.data.IMedia
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal fun CastViewModel.castPath(path: String) {
    val device = CastPlayer.currentDevice ?: return
    viewModelScope.launch(Dispatchers.IO) {
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

internal fun CastViewModel.castItem(item: IMedia) {
    val device = CastPlayer.currentDevice ?: return
    viewModelScope.launch(Dispatchers.IO) {
        CastPlayer.setCurrentUri(item.path)
        isLoading.value = true
        val castItems = CastPlayer.items.value
        val isInQueue = castItems.any { it.path == item.path }
        if (!isInQueue) {
            CastPlayer.addItem(item)
        }
        try {
            val mediaUrl = UrlHelper.getMediaHttpUrl(item.path)
            val albumArtUri = if (item is DAudio) UrlHelper.getAlbumArtHttpUrl(item.getAlbumUri()).toString() else ""
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

internal suspend fun CastViewModel.trySubscribeEvent() {
    val device = CastPlayer.currentDevice ?: return
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

internal fun CastViewModel.startPositionUpdater() {
    val device = CastPlayer.currentDevice ?: return
    if (!CastPlayer.supportsCallback.value) return

    positionUpdateJob?.cancel()

    positionUpdateJob = viewModelScope.launch(Dispatchers.IO) {
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
