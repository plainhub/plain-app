package com.ismartcoding.lib.kgraphql

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaConfigurationDSL

class KtorConfigurationDSL {
    /**
     * This adds support for opening the graphql route within the browser
     */
    var playground: Boolean = false

    var endpoint: String = "graphql"


    internal fun build() = KtorGraphQLConfiguration(
        playground = playground,
        endpoint = endpoint
    )
}

fun SchemaConfigurationDSL.ktor(block: KtorConfigurationDSL.() -> Unit) {
    val plugin = KtorConfigurationDSL().apply(block).build()
    install(plugin)
}
