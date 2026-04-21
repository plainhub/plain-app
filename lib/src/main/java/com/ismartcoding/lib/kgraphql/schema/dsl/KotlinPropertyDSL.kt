package com.ismartcoding.lib.kgraphql.schema.dsl

import com.ismartcoding.lib.kgraphql.Context
import com.ismartcoding.lib.kgraphql.schema.model.PropertyDef
import java.lang.IllegalArgumentException
import kotlin.reflect.KProperty1


class KotlinPropertyDSL<T : Any, R> (
        private val kProperty: KProperty1<T, R>,
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
            kProperty = kProperty,
            description = description,
            isDeprecated = isDeprecated,
            deprecationReason = deprecationReason,
            isIgnored = ignore,
            accessRule = accessRuleBlock
    )
}