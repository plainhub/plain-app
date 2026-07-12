package com.ismartcoding.plain.lib.kgraphql.schema.dsl

import com.ismartcoding.plain.lib.kgraphql.Context
import com.ismartcoding.plain.lib.kgraphql.schema.model.FunctionWrapper
import com.ismartcoding.plain.lib.kgraphql.schema.model.InputValueDef
import com.ismartcoding.plain.lib.kgraphql.schema.model.PropertyDef
import kotlin.reflect.KType


class PropertyDSL<T : Any, R>(val name : String, block : PropertyDSL<T, R>.() -> Unit) : LimitedAccessItemDSL<T>(), ResolverDSL.Target {

    internal lateinit var functionWrapper : FunctionWrapper<R>

    private val inputValues = mutableListOf<InputValueDef<*>>()

    var explicitReturnType: KType? = null

    init {
        block()
    }

    @PublishedApi
    internal fun resolver(function: FunctionWrapper<R>): ResolverDSL {
        functionWrapper = function
        return ResolverDSL(this)
    }

    // T is receiver, 0 GraphQL args
    inline fun <reified R2 : R> resolver(noinline function: suspend (T) -> R2) =
        resolver(FunctionWrapper.on(function))

    // T is receiver, 1 GraphQL arg
    inline fun <reified R2 : R, reified E> resolver(argName: String, noinline function: suspend (T, E) -> R2) =
        resolver(FunctionWrapper.on(argName, function))

    // T is receiver, 2 GraphQL args
    inline fun <reified R2 : R, reified E, reified W> resolver(argName1: String, argName2: String, noinline function: suspend (T, E, W) -> R2) =
        resolver(FunctionWrapper.on(argName1, argName2, function))

    // T is receiver, 3 GraphQL args
    inline fun <reified R2 : R, reified E, reified W, reified Q> resolver(argName1: String, argName2: String, argName3: String, noinline function: suspend (T, E, W, Q) -> R2) =
        resolver(FunctionWrapper.on(argName1, argName2, argName3, function))

    // T is receiver, 4 GraphQL args
    inline fun <reified R2 : R, reified E, reified W, reified Q, reified A> resolver(argName1: String, argName2: String, argName3: String, argName4: String, noinline function: suspend (T, E, W, Q, A) -> R2) =
        resolver(FunctionWrapper.on(argName1, argName2, argName3, argName4, function))

    // T is receiver, 5 GraphQL args
    inline fun <reified R2 : R, reified E, reified W, reified Q, reified A, reified S> resolver(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, noinline function: suspend (T, E, W, Q, A, S) -> R2) =
        resolver(FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, function))

    fun accessRule(rule: (T, Context) -> Exception?){

        val accessRuleAdapter: (T?, Context) -> Exception? = { parent, ctx ->
            if (parent != null) rule(parent, ctx) else IllegalArgumentException("Unexpected null parent of kotlin property")
        }

        this.accessRuleBlock = accessRuleAdapter
    }

    fun toKQLProperty() = PropertyDef.Function<T, R>(
            name = name,
            resolver = functionWrapper,
            description = description,
            isDeprecated = isDeprecated,
            deprecationReason = deprecationReason,
            inputValues = inputValues,
            accessRule = accessRuleBlock,
            explicitReturnType = explicitReturnType
    )

    override fun addInputValues(inputValues: Collection<InputValueDef<*>>) {
        this.inputValues.addAll(inputValues)
    }

    override fun setReturnType(type: KType) {
        explicitReturnType = type
    }
}
