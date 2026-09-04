/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.engine

import com.ismartcoding.plain.lib.ktorserver.core.application.*
import com.ismartcoding.plain.lib.ktorserver.core.config.*
import io.ktor.util.logging.*
import io.ktor.utils.io.*

/**
 * Engine environment configuration builder
 *
 */
@KtorDsl

public fun applicationEnvironment(
    block: ApplicationEnvironmentBuilder.() -> Unit = {}
): ApplicationEnvironment {
    return ApplicationEnvironmentBuilder().apply(block).build()
}

/**
 * Configures the application environment using the provided configuration file paths.
 *
 * If no paths are provided, the default configuration is loaded.
 * If one path is provided, the corresponding configuration file is loaded.
 * If multiple paths are provided, the configurations are merged in the given order.
 *
 *
 * @param configPaths Optional paths to configuration files.
 */
public fun ApplicationEnvironmentBuilder.configure(vararg configPaths: String) {
    config = ConfigLoader.loadAll(*configPaths)
}

/**
 * Configures the application environment builder by merging the provided configurations.
 *
 *
 * @param configs A variable number of [ApplicationConfig] instances to be merged and set as the builder's configuration.
 */
public fun ApplicationEnvironmentBuilder.configure(vararg configs: ApplicationConfig) {
    config = configs.reduce(ApplicationConfig::mergeWith)
}
