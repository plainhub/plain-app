package com.ismartcoding.lib.kgraphql.schema.model

import kotlin.reflect.KClass
import kotlin.reflect.KType


class InputValueDef<T : Any>(
        val kClass : KClass<T>,
        val name : String,
        val defaultValue : T? = null,
        override val isDeprecated: Boolean = false,
        override val description: String? = null,
        override val deprecationReason: String? = null,
        val kType : KType? = null
) : DescribedDef, Depreciable
