package com.ismartcoding.plain.features.dlna.sender

import com.ismartcoding.plain.lib.coIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object DlnaDeviceScanner {
    private val state = MutableStateFlow<List<DlnaDevice>>(emptyList())
    val devices = state.asStateFlow()
    fun start() { coIO { RustDlnaSender.call(CastCommand.StartScan) } }
    fun stop() { coIO { RustDlnaSender.call(CastCommand.StopScan) } }
    internal fun apply(devices: List<DlnaDevice>) { state.value = devices }
}
