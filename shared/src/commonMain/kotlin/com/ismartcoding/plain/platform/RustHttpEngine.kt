package com.ismartcoding.plain.platform

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.api.RustCoreBridge
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.preferences.UserPrefs
import kotlinx.coroutines.CancellationException
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.*

internal object RustHttpEngine {
    private val state = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isRunning: Boolean get() = state.value
    suspend fun start(): Boolean = withIO {
        try {
            RustContentApi.start()
            val config = PublicServerConfig(
                UserPrefs.httpPort.value,
                UserPrefs.httpsPort.value,
                isDebugBuild(),
                RustWebAssets.ensure(),
            )
            val ports = JsonHelper.jsonDecode<PublicServerPorts>(RustCoreBridge.startPublic(JsonHelper.jsonEncode(config)))
            state.value = true
            UserPrefs.httpPort.value = ports.httpPort
            UserPrefs.httpsPort.value = ports.httpsPort
            HttpServerManager.httpServerError.value = ""
            true
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (state.value) { RustCoreBridge.stopPublic(); state.value = false }
            HttpServerManager.httpServerError.value = error.message ?: "Rust HTTP server failed to start"
            LogCat.e("Rust HTTP server failed", error)
            false
        }
    }
    suspend fun stop() = withIO {
        if (state.value) { RustCoreBridge.stopPublic(); state.value = false }
    }
}
