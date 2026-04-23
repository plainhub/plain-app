package com.ismartcoding.lib.kgraphql.schema.introspection


interface __Schema {

    val types : List<__Type>

    val queryType : __Type

    val mutationType : __Type?

    val subscriptionType: __Type?

    val directives: List<__Directive>

    fun findTypeByName(name : String) : __Type?
}
