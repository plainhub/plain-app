package com.ismartcoding.plain.discover

import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import kotlinx.coroutines.*

object PairingInitiator {

    suspend fun start(device: DNearbyDevice) = withIO { RustPairingRuntime.start(device) }

    fun cancel(deviceId: String) { coIO { RustPairingRuntime.cancel(deviceId) } }
}
