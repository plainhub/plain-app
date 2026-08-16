package com.ismartcoding.plain.features.dlna.sender

import com.ismartcoding.plain.lib.dlna.common.DlnaDevice
import com.ismartcoding.plain.features.dlna.receiver.DlnaSsdpMessages
import com.ismartcoding.plain.features.dlna.receiver.DlnaSsdpSocket
import com.ismartcoding.plain.features.dlna.receiver.createDlnaSsdpSocket
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.IODispatcher
import com.ismartcoding.plain.platform.createHttpClient
import com.ismartcoding.plain.platform.getDeviceIP4s
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * SSDP 多播搜索得到的原始数据（来源 IP + 报文文本）。仅在本文件内部使用。
 */
private data class DlnaSsdpResponse(
    val hostAddress: String,
    val header: String,
)

object DlnaDeviceScanner {
    private val _devices = MutableStateFlow<List<DlnaDevice>>(emptyList())

    /**
     * 已发现的 DLNA 设备列表（已过滤本机、已去重、已确认支持 AVTransport）。
     * 页面直接订阅此流。
     */
    val devices: StateFlow<List<DlnaDevice>> = _devices.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + IODispatcher)
    private var scanJob: Job? = null
    private var socket: DlnaSsdpSocket? = null

    /**
     * 启动 SSDP 扫描。重复调用安全：若已在扫描则直接返回。
     *
     * 流程：M-SEARCH → 收到 SSDP 响应 → 过滤本机 + 去重 → 拉取设备描述 XML →
     * 判断是否支持 AVTransport → 加入 [devices]。任一步失败则丢弃该设备。
     */
    fun start() {
        if (scanJob?.isActive == true) return
        _devices.value = emptyList()
        scanJob = scope.launch {
            val localIps = getDeviceIP4s().toHashSet()
            val sock = createDlnaSsdpSocket(bindPort = 0) ?: run {
                LogCat.e("DLNA scanner: failed to create SSDP socket")
                return@launch
            }
            socket = sock
            try {
                sock.sendMulticast(DlnaSsdpMessages.M_SEARCH_QUERY)
                LogCat.d("DLNA scanner: sent M-SEARCH to ${DlnaSsdpMessages.SSDP_ADDR}:${DlnaSsdpMessages.SSDP_PORT}")
                while (isActive) {
                    val packet = sock.receive(5_000)
                    if (packet == null) {
                        // Timeout — resend M-SEARCH
                        sock.sendMulticast(DlnaSsdpMessages.M_SEARCH_QUERY)
                        continue
                    }
                    val prefix = packet.message.take(20).uppercase()
                    if (prefix.startsWith("HTTP/1.1 200") || prefix.startsWith("NOTIFY * HTTP")) {
                        LogCat.d("DLNA scanner: received response from ${packet.sourceAddress}")
                        onResponse(DlnaSsdpResponse(packet.sourceAddress, packet.message), localIps)
                    }
                }
            } catch (e: Exception) {
                LogCat.e("DLNA scanner error: ${e.message}")
            }
        }
    }

    private suspend fun onResponse(ssdp: DlnaSsdpResponse, localIps: Set<String>) {
        if (ssdp.hostAddress in localIps) return
        if (_devices.value.any { it.hostAddress == ssdp.hostAddress }) return
        val device = DlnaDevice(ssdp.hostAddress, ssdp.header)
        try {
            val client = createHttpClient()
            val response = client.get(device.location)
            if (response.status != HttpStatusCode.OK) return
            val xml = response.body<String>()
            device.update(xml)
            if (!device.isAVTransport()) return
        } catch (e: Exception) {
            LogCat.e("DLNA scanner: failed to fetch description for ${ssdp.hostAddress}: ${e.message}")
            return
        }
        _devices.value = _devices.value + device
    }

    /**
     * 停止 SSDP 扫描并关闭 socket。离开页面时调用以释放网络资源。
     */
    fun stop() {
        scanJob?.cancel()
        scanJob = null
        socket?.close()
        socket = null
        LogCat.d("DLNA scanner: stopped and socket closed")
    }
}

