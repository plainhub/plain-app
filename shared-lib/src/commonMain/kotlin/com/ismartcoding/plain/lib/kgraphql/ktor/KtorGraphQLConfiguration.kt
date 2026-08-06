package com.ismartcoding.plain.lib.kgraphql

import com.ismartcoding.plain.lib.kgraphql.configuration.PluginConfiguration

class KtorGraphQLConfiguration(
    val playground: Boolean,
    val endpoint: String
): PluginConfiguration
