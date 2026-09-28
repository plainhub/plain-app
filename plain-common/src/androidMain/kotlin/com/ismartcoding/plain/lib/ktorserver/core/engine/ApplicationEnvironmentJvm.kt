/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.engine

import com.ismartcoding.plain.lib.ktorserver.events.*
import com.ismartcoding.plain.lib.ktorserver.core.application.*
import com.ismartcoding.plain.lib.ktorserver.core.config.*
import io.ktor.utils.io.*
import org.slf4j.*

/**
 * Builder for configuring the environment of the Ktor application.
 *
 */
@KtorDsl
public class ApplicationEnvironmentBuilder {
    /**
     * Root class loader.
     *
     */
    public var classLoader: ClassLoader = ApplicationEnvironmentBuilder::class.java.classLoader

    /**
     * Application logger.
     *
     */
    public var log: Logger = LoggerFactory.getLogger("io.ktor.server.Application")

    /**
     * Configuration for the application.
     *
     */
    public var config: ApplicationConfig = MapApplicationConfig()

    /**
     * Builds and returns an instance of the application engine environment based on the configured settings.
     *
     */
    public fun build(): ApplicationEnvironment {
        return ApplicationEnvironmentImplJvm(classLoader, log, config)
    }
}

internal class ApplicationEnvironmentImplJvm(
    override val classLoader: ClassLoader,
    override val log: Logger,
    override val config: ApplicationConfig,
    @Deprecated(
        "Moved to Application",
        replaceWith = ReplaceWith("EmbeddedServer.monitor", "io.ktor.server.engine.EmbeddedServer"),
        level = DeprecationLevel.WARNING
    )
    override val monitor: Events = Events()
) : ApplicationEnvironment
