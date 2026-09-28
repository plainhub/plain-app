package com.ismartcoding.plain.lib.kgraphql.schema.introspection

import com.ismartcoding.plain.lib.kgraphql.schema.model.Depreciable


interface __Field : Depreciable, __Described {

    val type: __Type

    val args: List<__InputValue>
}