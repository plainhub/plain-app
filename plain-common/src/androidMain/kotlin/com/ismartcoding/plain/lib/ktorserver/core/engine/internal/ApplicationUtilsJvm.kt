/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.engine.internal

import com.ismartcoding.plain.lib.ktorserver.core.application.*
import com.ismartcoding.plain.lib.ktorserver.core.config.*
import com.ismartcoding.plain.lib.ktorserver.core.engine.*
import kotlinx.coroutines.*

internal fun availableProcessorsBridge(): Int = Runtime.getRuntime().availableProcessors()

internal val Dispatchers.IOBridge: CoroutineDispatcher get() = IO

internal fun printError(message: Any?) {
    System.err.print(message)
}

internal fun configureShutdownUrl(config: ApplicationConfig, pipeline: EnginePipeline) {
    val url = config.propertyOrNull("ktor.deployment.shutdown.url")?.getString() ?: return
    pipeline.install(ShutDownUrl.EnginePlugin) {
        shutDownUrl = url
    }
}
