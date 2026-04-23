package com.ismartcoding.lib.kgraphql.schema.dsl.types

import com.ismartcoding.lib.kgraphql.defaultKQLTypeName
import com.ismartcoding.lib.kgraphql.schema.dsl.ItemDSL
import kotlin.reflect.KClass


class InputTypeDSL<T : Any>(val kClass: KClass<T>) : ItemDSL() {

    var name = kClass.defaultKQLTypeName()
}
