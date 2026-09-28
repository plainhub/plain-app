/*
 * Copyright 2014-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.routing

import com.ismartcoding.plain.lib.ktorserver.core.application.*

/**
 * Gets the root of the routing block.
 *
 */
public val Application.routingRoot: RoutingNode
    get() = pluginOrNull(RoutingRoot) ?: throw IllegalStateException("Routing plugin is not installed")
