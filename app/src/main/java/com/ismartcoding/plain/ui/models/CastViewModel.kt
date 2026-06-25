package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.lib.extensions.getFilenameWithoutExtensionFromPath
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.features.dlna.common.DlnaDevice
import com.ismartcoding.plain.features.dlna.sender.DlnaTransportController
import com.ismartcoding.plain.features.dlna.sender.DlnaDeviceScanner
import com.ismartcoding.plain.data.IMedia
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class CastViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<DlnaDevice>>(emptyList())
    val itemsFlow: StateFlow<List<DlnaDevice>> = _itemsFlow
    var castMode = mutableStateOf(false)
    var showCastDialog = mutableStateOf(false)
    val isLoading = mutableStateOf(false)

    internal var positionUpdateJob: Job? = null

    fun enterCastMode() {
        castMode.value = true
        showCastDialog.value = false
    }

    fun selectDevice(device: DlnaDevice) {
        CastPlayer.currentDevice = device
    }

    fun exitCastMode() {
        castMode.value = false
        val device = CastPlayer.currentDevice ?: return
        launchSafe {
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

    suspend fun searchAsync(context: Context) {
        DlnaDeviceScanner.search(context).flowOn(Dispatchers.IO).buffer().collect { device ->
            try {
                val client = HttpClient(CIO)
                val response = withIO { client.get(device.location) }
                if (response.status != HttpStatusCode.OK) {
                    return@collect
                }
                val xml = response.body<String>()
                LogCat.e(xml)
                device.update(xml)
                if (device.isAVTransport()) {
                    addDevice(device)
                }
            } catch (ex: Exception) {
                LogCat.e(ex.toString())
            }
        }
    }

    private fun addDevice(device: DlnaDevice) {
        if (!_itemsFlow.value.any { it.hostAddress == device.hostAddress }) {
            _itemsFlow.update { it + device }
        }
    }

    fun playCast() {
        val device = CastPlayer.currentDevice ?: return
        launchSafe {
            DlnaTransportController.playAVTransportAsync(device)
            CastPlayer.isPlaying.value = true
        }
    }

    fun pauseCast() {
        val device = CastPlayer.currentDevice ?: return
        launchSafe {
            DlnaTransportController.pauseAVTransportAsync(device)
            CastPlayer.isPlaying.value = false
        }
    }

    fun castPath(path: String) {
        val device = CastPlayer.currentDevice ?: return
        launchSafe {
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
        launchSafe {
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

        positionUpdateJob = launchSafe {
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
