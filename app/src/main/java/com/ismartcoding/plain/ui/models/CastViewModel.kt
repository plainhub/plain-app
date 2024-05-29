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
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.helpers.UrlHelper
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
import kotlinx.coroutines.launch

class CastViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<UPnPDevice>())
    val itemsFlow: StateFlow<List<UPnPDevice>> get() = _itemsFlow
    var castMode = mutableStateOf(false)
    var showCastDialog = mutableStateOf(false)

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
        }
    }

    fun cast(path: String) {
        val device = CastPlayer.currentDevice ?: return
        viewModelScope.launch(Dispatchers.IO) {
            CastPlayer.currentUri = path
            UPnPController.setAVTransportURIAsync(device, UrlHelper.getMediaHttpUrl(path))
            if (CastPlayer.sid.isNotEmpty()) {
                UPnPController.unsubscribeEvent(device, CastPlayer.sid)
                CastPlayer.sid = ""
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
}
