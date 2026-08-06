package com.ismartcoding.plain.lib.kgraphql.schema.dsl

import com.ismartcoding.plain.lib.kgraphql.Context
import com.ismartcoding.plain.lib.kgraphql.schema.model.FunctionWrapper
import com.ismartcoding.plain.lib.kgraphql.schema.model.InputValueDef
import com.ismartcoding.plain.lib.kgraphql.schema.model.PropertyDef
import com.ismartcoding.plain.lib.kgraphql.schema.model.TypeDef
import kotlin.reflect.KType


class UnionPropertyDSL<T : Any>(val name : String, block: UnionPropertyDSL<T>.() -> Unit) : LimitedAccessItemDSL<T>(), ResolverDSL.Target {

    init {
        block()
    }

    internal lateinit var functionWrapper : FunctionWrapper<Any?>

    lateinit var returnType : TypeID

    var nullable: Boolean = false

    private val inputValues = mutableListOf<InputValueDef<*>>()

    @PublishedApi
    internal fun resolver(function: FunctionWrapper<Any?>): ResolverDSL {
        functionWrapper = function
        return ResolverDSL(this)
    }

    // T is receiver, 0 GraphQL args
    inline fun <reified R2> resolver(noinline function: suspend (T) -> R2) =
        resolver(FunctionWrapper.on(function))

    // T is receiver, 1 GraphQL arg
    inline fun <reified R2, reified E> resolver(argName: String, noinline function: suspend (T, E) -> R2) =
        resolver(FunctionWrapper.on(argName, function))

    // T is receiver, 2 GraphQL args
    inline fun <reified R2, reified E, reified W> resolver(argName1: String, argName2: String, noinline function: suspend (T, E, W) -> R2) =
        resolver(FunctionWrapper.on(argName1, argName2, function))

    // T is receiver, 3 GraphQL args
    inline fun <reified R2, reified E, reified W, reified Q> resolver(argName1: String, argName2: String, argName3: String, noinline function: suspend (T, E, W, Q) -> R2) =
        resolver(FunctionWrapper.on(argName1, argName2, argName3, function))

    // T is receiver, 4 GraphQL args
    inline fun <reified R2, reified E, reified W, reified Q, reified A> resolver(argName1: String, argName2: String, argName3: String, argName4: String, noinline function: suspend (T, E, W, Q, A) -> R2) =
        resolver(FunctionWrapper.on(argName1, argName2, argName3, argName4, function))

    // T is receiver, 5 GraphQL args
    inline fun <reified R2, reified E, reified W, reified Q, reified A, reified S> resolver(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, noinline function: suspend (T, E, W, Q, A, S) -> R2) =
        resolver(FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, function))

    // T is receiver, 6 GraphQL args
    inline fun <reified R2, reified E, reified W, reified Q, reified A, reified S, reified B> resolver(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, noinline function: suspend (T, E, W, Q, A, S, B) -> R2) =
        resolver(FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, argName6, function))

    // T is receiver, 7 GraphQL args
    inline fun <reified R2, reified E, reified W, reified Q, reified A, reified S, reified B, reified U> resolver(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, argName7: String, noinline function: suspend (T, E, W, Q, A, S, B, U) -> R2) =
        resolver(FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, argName6, argName7, function))

    // T is receiver, 8 GraphQL args
    inline fun <reified R2, reified E, reified W, reified Q, reified A, reified S, reified B, reified U, reified C> resolver(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, argName7: String, argName8: String, noinline function: suspend (T, E, W, Q, A, S, B, U, C) -> R2) =
        resolver(FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, argName6, argName7, argName8, function))

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
