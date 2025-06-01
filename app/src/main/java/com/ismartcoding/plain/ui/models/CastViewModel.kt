package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.lib.upnp.UPnPController
import com.ismartcoding.lib.upnp.UPnPDevice
import com.ismartcoding.lib.upnp.UPnPDiscovery
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class CastViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<UPnPDevice>())
    val itemsFlow: StateFlow<List<UPnPDevice>> get() = _itemsFlow
    var castMode = mutableStateOf(false)
    var showCastDialog = mutableStateOf(false)
    val isLoading = mutableStateOf(false)
    
    private var positionUpdateJob: Job? = null

    fun enterCastMode() {
        castMode.value = true
        showCastDialog.value = true
    }

    fun selectDevice(device: UPnPDevice) {
        CastPlayer.currentDevice = device
    }

    fun exitCastMode() {
        castMode.value = false
        val device = CastPlayer.currentDevice ?: return
        viewModelScope.launch(Dispatchers.IO) {
            UPnPController.stopAVTransportAsync(device)
            CastPlayer.isPlaying.value = false
            
            // 清理投屏状态
            if (CastPlayer.sid.isNotEmpty()) {
                UPnPController.unsubscribeEvent(device, CastPlayer.sid)
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

    fun cast(path: String) {
        val device = CastPlayer.currentDevice ?: return
        viewModelScope.launch(Dispatchers.IO) {
            isLoading.value = true
            CastPlayer.setCurrentUri(path)
            try {
                UPnPController.setAVTransportURIAsync(device, UrlHelper.getMediaHttpUrl(path))
                CastPlayer.isPlaying.value = true
                if (CastPlayer.sid.isNotEmpty()) {
                    UPnPController.unsubscribeEvent(device, CastPlayer.sid)
                    CastPlayer.sid = ""
                }
                
                // 尝试订阅事件回调
                trySubscribeEvent()
            } catch (e: Exception) {
                DialogHelper.showErrorMessage(e.message ?: "Cast failed")
            } finally {
                isLoading.value = false
            }
        }
    }

    fun cast(item: IMedia) {
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
                UPnPController.setAVTransportURIAsync(device, UrlHelper.getMediaHttpUrl(item.path))
                CastPlayer.isPlaying.value = true
                if (CastPlayer.sid.isNotEmpty()) {
                    UPnPController.unsubscribeEvent(device, CastPlayer.sid)
                    CastPlayer.sid = ""
                }
                
                // 尝试订阅事件回调
                trySubscribeEvent()
            } catch (e: Exception) {
                DialogHelper.showErrorMessage(e.message ?: "Cast failed")
            } finally {
                isLoading.value = false
            }
        }
    }

    private suspend fun trySubscribeEvent() {
        val device = CastPlayer.currentDevice ?: return
        try {
            val sid = UPnPController.subscribeEvent(device, UrlHelper.getCastCallbackUrl())
            if (sid.isNotEmpty()) {
                CastPlayer.sid = sid
                CastPlayer.supportsCallback.value = true
                // 开始定期获取播放位置
                startPositionUpdater()
            } else {
                CastPlayer.supportsCallback.value = false
            }
        } catch (e: Exception) {
            CastPlayer.supportsCallback.value = false
        }
    }

    private fun startPositionUpdater() {
        val device = CastPlayer.currentDevice ?: return
        if (!CastPlayer.supportsCallback.value) return
        
        // 取消之前的作业
        positionUpdateJob?.cancel()
        
        positionUpdateJob = viewModelScope.launch(Dispatchers.IO) {
            while (CastPlayer.currentUri.value.isNotEmpty() && CastPlayer.supportsCallback.value) {
                try {
                    if (CastPlayer.isPlaying.value) {
                        val positionInfo = UPnPController.getPositionInfoAsync(device)
                        CastPlayer.updatePositionInfo(positionInfo.relTime, positionInfo.trackDuration)
                    }
                } catch (e: Exception) {
                    // 获取位置信息失败，可能不支持
                    break
                }
                delay(1000) // 每秒更新一次
            }
        }
    }

    suspend fun searchAsync(context: Context) {
        UPnPDiscovery.search(context).flowOn(Dispatchers.IO).buffer().collect { device ->
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

    private fun addDevice(device: UPnPDevice) {
        if (!_itemsFlow.value.any { it.hostAddress == device.hostAddress }) {
            _itemsFlow.value.add(device)
        }
    }

    fun playCast() {
        val device = CastPlayer.currentDevice ?: return
        viewModelScope.launch(Dispatchers.IO) {
            UPnPController.playAVTransportAsync(device)
            CastPlayer.isPlaying.value = true
        }
    }

    fun pauseCast() {
        val device = CastPlayer.currentDevice ?: return
        viewModelScope.launch(Dispatchers.IO) {
            UPnPController.pauseAVTransportAsync(device)
            CastPlayer.isPlaying.value = false
        }
    }
}
