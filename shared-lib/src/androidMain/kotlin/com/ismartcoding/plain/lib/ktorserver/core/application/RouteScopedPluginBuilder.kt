/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.application

import com.ismartcoding.plain.lib.ktorserver.core.routing.*
import io.ktor.util.*

/**
 * Utility class to build a [RouteScopedPlugin] instance.
 *
 **/
public abstract class RouteScopedPluginBuilder<PluginConfig : Any>(key: AttributeKey<PluginInstance>) :
    PluginBuilder<PluginConfig>(key) {

    /**
     * A [RoutingNode] to which this plugin was installed. Can be `null` if plugin in installed into [Application].
     *
     **/
    public abstract val route: RoutingNode?
}
