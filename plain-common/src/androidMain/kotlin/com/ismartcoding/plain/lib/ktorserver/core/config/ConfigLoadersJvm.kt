/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.config

import com.ismartcoding.plain.lib.ktorserver.core.engine.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*

internal val CONFIG_PATH: List<String>
    get() = listOfNotNull(
        getEnvironmentProperty("config.file"),
        getEnvironmentProperty("config.resource"),
        getEnvironmentProperty("config.url"),
    )

@OptIn(InternalAPI::class)
public val configLoaders: List<ConfigLoader> = loadServices<ConfigLoader>()
