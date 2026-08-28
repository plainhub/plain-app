package com.ismartcoding.plain.lib.kgraphql.schema.dsl

import com.ismartcoding.plain.lib.kgraphql.Context
import com.ismartcoding.plain.lib.kgraphql.schema.model.PropertyDef
import kotlin.reflect.KType


class KotlinPropertyDSL<T : Any, R> (
        private val name: String,
        private val accessor: (T) -> R,
        private val returnType: KType? = null,
        block : KotlinPropertyDSL<T, R>.() -> Unit
) : LimitedAccessItemDSL<T>(){

    var ignore = false

    init {
        block()
    }

    fun accessRule(rule: (T, Context) -> Exception?){

        val accessRuleAdapter: (T?, Context) -> Exception? = { parent, ctx ->
            if (parent != null) rule(parent, ctx) else IllegalArgumentException("Unexpected null parent of kotlin property")
        }

        this.accessRuleBlock = accessRuleAdapter
    }

    fun toKQLProperty() = PropertyDef.Kotlin (
            name = name,
            accessor = accessor,
            returnType = returnType,
            description = description,
            isDeprecated = isDeprecated,
            deprecationReason = deprecationReason,
            isIgnored = ignore,
            accessRule = accessRuleBlock
    )
}
