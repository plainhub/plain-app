package com.ismartcoding.plain.lib.kgraphql.schema.model


interface Depreciable {

    val isDeprecated: Boolean

    val deprecationReason : String?
}