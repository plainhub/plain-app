package com.ismartcoding.plain.features.dlna.sender

import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.features.dlna.common.DlnaDevice
import com.ismartcoding.plain.platform.searchDlnaDevicesRaw
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

object DlnaDeviceScanner {
    private val devices = HashSet<DlnaDevice>()

    /**
     * 发起 SSDP 搜索并返回发现的 DLNA 设备流。
     * 平台层只负责 UDP 多播收发（searchDlnaDevicesRaw）；
     * 本层负责基于 hostAddress 去重并构造 DlnaDevice（解析 SSDP header）。
     * 设备描述 XML 的拉取与 AVTransport 判断由调用方（CastViewModel）负责。
     */
    fun search(): Flow<DlnaDevice> = searchDlnaDevicesRaw().transform { ssdp ->
        if (devices.none { it.hostAddress == ssdp.hostAddress }) {
            LogCat.d(ssdp.header)
            val device = DlnaDevice(ssdp.hostAddress, ssdp.header)
            devices.add(device)
            emit(device)
        }
    }
}
