package com.ismartcoding.plain.lib.kgraphql.schema.dsl

import com.ismartcoding.plain.lib.kgraphql.Context
import com.ismartcoding.plain.lib.kgraphql.schema.model.FunctionWrapper
import com.ismartcoding.plain.lib.kgraphql.schema.model.InputValueDef
import com.ismartcoding.plain.lib.kgraphql.schema.model.PropertyDef
import com.ismartcoding.plain.lib.kdataloader.BatchLoader
import com.ismartcoding.plain.lib.kdataloader.TimedAutoDispatcherDataLoaderOptions
import com.ismartcoding.plain.lib.kdataloader.factories.TimedAutoDispatcherDataLoaderFactory
import kotlin.reflect.KType

class DataLoaderPropertyDSL<T, K, R>(
    val name: String,
    val returnType: KType,
    private val block : DataLoaderPropertyDSL<T, K, R>.() -> Unit
): LimitedAccessItemDSL<T>(), ResolverDSL.Target {

    internal var dataLoader: BatchLoader<K, R>? = null

    @PublishedApi
    internal var prepareWrapper: FunctionWrapper<K>? = null

    private val inputValues = mutableListOf<InputValueDef<*>>()

    var explicitReturnType: KType? = null

    fun loader(block: BatchLoader<K, R>) {
        dataLoader = block
    }

    // T is receiver, 0 GraphQL args
    inline fun <reified K2 : K> prepare(noinline block: suspend (T) -> K2) {
        prepareWrapper = FunctionWrapper.on(block)
    }

    // T is receiver, 1 GraphQL arg
    inline fun <reified K2 : K, reified E> prepare(argName: String, noinline block: suspend (T, E) -> K2) {
        prepareWrapper = FunctionWrapper.on(argName, block)
    }

    // T is receiver, 2 GraphQL args
    inline fun <reified K2 : K, reified E, reified W> prepare(argName1: String, argName2: String, noinline block: suspend (T, E, W) -> K2) {
        prepareWrapper = FunctionWrapper.on(argName1, argName2, block)
    }

    // T is receiver, 3 GraphQL args
    inline fun <reified K2 : K, reified E, reified W, reified Q> prepare(argName1: String, argName2: String, argName3: String, noinline block: suspend (T, E, W, Q) -> K2) {
        prepareWrapper = FunctionWrapper.on(argName1, argName2, argName3, block)
    }

    // T is receiver, 4 GraphQL args
    inline fun <reified K2 : K, reified E, reified W, reified Q, reified A> prepare(argName1: String, argName2: String, argName3: String, argName4: String, noinline block: suspend (T, E, W, Q, A) -> K2) {
        prepareWrapper = FunctionWrapper.on(argName1, argName2, argName3, argName4, block)
    }

    // T is receiver, 5 GraphQL args
    inline fun <reified K2 : K, reified E, reified W, reified Q, reified A, reified S> prepare(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, noinline block: suspend (T, E, W, Q, A, S) -> K2) {
        prepareWrapper = FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, block)
    }

    // T is receiver, 6 GraphQL args
    inline fun <reified K2 : K, reified E, reified W, reified Q, reified A, reified S, reified B> prepare(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, noinline block: suspend (T, E, W, Q, A, S, B) -> K2) {
        prepareWrapper = FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, argName6, block)
    }

    // T is receiver, 7 GraphQL args
    inline fun <reified K2 : K, reified E, reified W, reified Q, reified A, reified S, reified B, reified U> prepare(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, argName7: String, noinline block: suspend (T, E, W, Q, A, S, B, U) -> K2) {
        prepareWrapper = FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, argName6, argName7, block)
    }

    // T is receiver, 8 GraphQL args
    inline fun <reified K2 : K, reified E, reified W, reified Q, reified A, reified S, reified B, reified U, reified C> prepare(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, argName7: String, argName8: String, noinline block: suspend (T, E, W, Q, A, S, B, U, C) -> K2) {
        prepareWrapper = FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, argName6, argName7, argName8, block)
    }

    fun accessRule(rule: (T, Context) -> Exception?){
        val accessRuleAdapter: (T?, Context) -> Exception? = { parent, ctx ->
            if (parent != null) rule(parent, ctx) else IllegalArgumentException("Unexpected null parent of kotlin property")
        }
        this.accessRuleBlock = accessRuleAdapter
    }

    fun toKQLProperty(): PropertyDef.DataLoadedFunction<T, K, R> {
        block()
        requireNotNull(prepareWrapper)
        requireNotNull(dataLoader)

        return PropertyDef.DataLoadedFunction(
            name = name,
            description = description,
            accessRule = accessRuleBlock,
            deprecationReason = deprecationReason,
            isDeprecated = isDeprecated,
            inputValues = inputValues,
            returnType = explicitReturnType ?: returnType,
            prepare = prepareWrapper!!,
            loader = TimedAutoDispatcherDataLoaderFactory(
                { TimedAutoDispatcherDataLoaderOptions() },
                mapOf(),
                dataLoader!!,
            )
        )
    }

    override fun addInputValues(inputValues: Collection<InputValueDef<*>>) {
        this.inputValues.addAll(inputValues)
    }

    override fun setReturnType(type: KType) {
        explicitReturnType = type
    }

}
