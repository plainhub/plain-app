package com.ismartcoding.plain.ui.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.lib.upnp.UPnPDevice
import com.ismartcoding.lib.upnp.UPnPDiscovery
import com.ismartcoding.plain.MainApp
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flowOn

class CastViewModelOld : ViewModel() {
    val devices: LiveData<List<UPnPDevice>>
        get() = mDevices

    private val mDevices = MutableLiveData<List<UPnPDevice>>()

    private fun addDevice(device: UPnPDevice) {
        if (mDevices.value == null || mDevices.value?.any { it.hostAddress == device.hostAddress } == false) {
            val values = mDevices.value?.toMutableList() ?: arrayListOf()
            values.add(device)
            mDevices.value = values
        }
    }

    suspend fun searchAsync() {
        UPnPDiscovery.search(MainApp.instance).flowOn(Dispatchers.IO).buffer().collect { device ->
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
}
