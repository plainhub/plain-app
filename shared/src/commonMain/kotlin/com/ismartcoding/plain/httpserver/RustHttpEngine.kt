package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.api.RustCoreBridge
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.preferences.UserPrefs
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*

internal object RustHttpEngine {
    private val state = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isRunning: Boolean get() = state.value
    suspend fun start(): Boolean = withIO {
        try {
            RustContentApi.start()
            val config = buildJsonObject {
                put("httpPort", UserPrefs.httpPort.value)
                put("httpsPort", UserPrefs.httpsPort.value)
            }
            val ports = Json.parseToJsonElement(RustCoreBridge.startPublic(config.toString())).jsonObject
            state.value = true
            UserPrefs.httpPort.value = ports.getValue("httpPort").jsonPrimitive.int
            UserPrefs.httpsPort.value = ports.getValue("httpsPort").jsonPrimitive.int
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
