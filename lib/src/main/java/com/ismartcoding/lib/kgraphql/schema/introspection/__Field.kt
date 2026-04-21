package com.ismartcoding.lib.kgraphql.schema.introspection

import com.ismartcoding.lib.kgraphql.schema.model.Depreciable


interface __Field : Depreciable, __Described {

    val type: __Type

    val args: List<__InputValue>
}