package com.ismartcoding.lib.kgraphql

import com.ismartcoding.lib.kgraphql.configuration.PluginConfiguration

class KtorGraphQLConfiguration(
    val playground: Boolean,
    val endpoint: String
): PluginConfiguration
