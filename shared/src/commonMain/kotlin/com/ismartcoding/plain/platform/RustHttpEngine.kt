package com.ismartcoding.plain.platform

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.lib.withIO

internal object RustHttpEngine {
    val isRunning: Boolean get() = RustContentApi.httpGeneration != 0L
    suspend fun start(): Unit = withIO { RustContentApi.start() }
    suspend fun stop(): Unit = withIO { RustContentApi.stopHttp() }
    fun failed(serverGeneration: Long): Boolean = RustContentApi.httpFailed(serverGeneration)
}
