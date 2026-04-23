package com.ismartcoding.lib.kgraphql.schema.dsl

import com.ismartcoding.lib.kgraphql.Context
import com.ismartcoding.lib.kgraphql.schema.model.FunctionWrapper
import com.ismartcoding.lib.kgraphql.schema.model.InputValueDef
import com.ismartcoding.lib.kgraphql.schema.model.PropertyDef
import com.ismartcoding.lib.kgraphql.schema.model.TypeDef
import java.lang.IllegalArgumentException
import kotlin.reflect.KType


class UnionPropertyDSL<T : Any>(val name : String, block: UnionPropertyDSL<T>.() -> Unit) : LimitedAccessItemDSL<T>(), ResolverDSL.Target {

    init {
        block()
    }

    internal lateinit var functionWrapper : FunctionWrapper<Any?>

    lateinit var returnType : TypeID

    var nullable: Boolean = false

    private val inputValues = mutableListOf<InputValueDef<*>>()

    private fun resolver(function: FunctionWrapper<Any?>): ResolverDSL {
        functionWrapper = function
        return ResolverDSL(this)
    }

    fun resolver(function: suspend (T) -> Any?) = resolver(FunctionWrapper.on(function, true))

    fun <E> resolver(function: suspend (T, E) -> Any?) = resolver(FunctionWrapper.on(function, true))

    fun <E, W> resolver(function: suspend (T, E, W) -> Any?) = resolver(FunctionWrapper.on(function, true))

    fun <E, W, Q> resolver(function: suspend (T, E, W, Q) -> Any?) = resolver(FunctionWrapper.on(function, true))

    fun <E, W, Q, A> resolver(function: suspend (T, E, W, Q, A) -> Any?) = resolver(FunctionWrapper.on(function, true))

    fun <E, W, Q, A, S> resolver(function: suspend (T, E, W, Q, A, S) -> Any?) = resolver(FunctionWrapper.on(function, true))

    fun <E, W, Q, A, S, B> resolver(function: suspend (T, E, W, Q, A, S, B) -> Any?) = resolver(FunctionWrapper.on(function, true))

    fun <E, W, Q, A, S, B, U> resolver(function: suspend (T, E, W, Q, A, S, B, U) -> Any?) = resolver(FunctionWrapper.on(function, true))

    fun <E, W, Q, A, S, B, U, C> resolver(function: suspend (T, E, W, Q, A, S, B, U, C) -> Any?) = resolver(FunctionWrapper.on(function, true))

    fun accessRule(rule: (T, Context) -> Exception?){

        val accessRuleAdapter: (T?, Context) -> Exception? = { parent, ctx ->
            if (parent != null) rule(parent, ctx) else IllegalArgumentException("Unexpected null parent of kotlin property")
        }

        this.accessRuleBlock = accessRuleAdapter
    }

    fun toKQLProperty(union : TypeDef.Union) = PropertyDef.Union<T> (
        name = name,
        resolver = functionWrapper,
        union = union,
        description = description,
        nullable = nullable,
        isDeprecated = isDeprecated,
        deprecationReason = deprecationReason,
        inputValues = inputValues,
        accessRule = accessRuleBlock
    )

    override fun addInputValues(inputValues: Collection<InputValueDef<*>>) {
        this.inputValues.addAll(inputValues)
    }

    override fun setReturnType(type: KType) {
        throw IllegalArgumentException("A return value cannot be set on an Union type")
    }
}
