/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-netty:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver

import io.ktor.events.*
import io.ktor.server.application.*
import io.ktor.server.engine.*

/**
 * An [ApplicationEngineFactory] providing a Netty-based [ApplicationEngine]
 *
 */
public object Netty : ApplicationEngineFactory<NettyApplicationEngine, NettyApplicationEngine.Configuration> {

    override fun configuration(
        configure: NettyApplicationEngine.Configuration.() -> Unit
    ): NettyApplicationEngine.Configuration {
        return NettyApplicationEngine.Configuration().apply(configure)
    }

    override fun create(
        environment: ApplicationEnvironment,
        monitor: Events,
        developmentMode: Boolean,
        configuration: NettyApplicationEngine.Configuration,
        applicationProvider: () -> Application
    ): NettyApplicationEngine {
        return NettyApplicationEngine(environment, monitor, developmentMode, configuration, applicationProvider)
    }
}
