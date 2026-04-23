package com.ismartcoding.lib.kgraphql.schema.dsl


abstract class DepreciableItemDSL : ItemDSL() {

    internal var isDeprecated = false

    internal var deprecationReason: String? = null

    infix fun deprecate(reason: String?){
        isDeprecated = true
        deprecationReason = reason
    }
}