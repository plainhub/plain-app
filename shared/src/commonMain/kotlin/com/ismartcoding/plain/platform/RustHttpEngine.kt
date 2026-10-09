package com.ismartcoding.plain.platform

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.api.RustCoreBridge
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.preferences.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow

internal object RustHttpEngine {
    private val generation = MutableStateFlow(0L)
    val isRunning: Boolean get() = generation.value != 0L

    suspend fun start(): Unit = withIO {
        RustContentApi.start()
        val config = PublicServerConfig(
            UserPrefs.httpPort.value,
            UserPrefs.httpsPort.value,
            isDebugBuild(),
            RustWebAssets.ensure(),
        )
        val result = JsonHelper.jsonDecode<PublicServerPorts>(RustCoreBridge.startPublic(JsonHelper.jsonEncode(config)))
        check(result.errorCode.isEmpty()) { result.error }
        generation.value = result.generation
        UserPrefs.httpPort.value = result.httpPort
        UserPrefs.httpsPort.value = result.httpsPort
    }

    suspend fun stop(): Unit = withIO {
        RustCoreBridge.stopPublic()
        generation.value = 0L
    }

    fun failed(serverGeneration: Long): Boolean {
        if (!isRunning || generation.value != serverGeneration) return false
        generation.value = 0L
        return true
    }
}
