package com.ismartcoding.lib.kgraphql.schema.model


interface Depreciable {

    val isDeprecated: Boolean

    val deprecationReason : String?
}