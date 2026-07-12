package com.ismartcoding.plain.lib.kgraphql.schema.dsl.operations

import com.ismartcoding.plain.lib.kgraphql.Context
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.LimitedAccessItemDSL
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.ResolverDSL
import com.ismartcoding.plain.lib.kgraphql.schema.model.FunctionWrapper
import com.ismartcoding.plain.lib.kgraphql.schema.model.InputValueDef
import kotlin.reflect.KType


abstract class AbstractOperationDSL(
    val name: String
) : LimitedAccessItemDSL<Nothing>(),
    ResolverDSL.Target {

    protected val inputValues = mutableListOf<InputValueDef<*>>()

    internal var functionWrapper: FunctionWrapper<*>? = null

    var explicitReturnType: KType? = null

    @PublishedApi
    internal fun resolver(function: FunctionWrapper<*>): ResolverDSL {
        require(function.hasReturnType()) {
            "Resolver for '$name' has no return value"
        }
        functionWrapper = function
        return ResolverDSL(this)
    }

    inline fun <reified T> resolver(noinline function: suspend () -> T) = resolver(FunctionWrapper.on(function))

    inline fun <reified T, reified R> resolver(argName: String, noinline function: suspend (R) -> T) =
        resolver(FunctionWrapper.on(argName, function))

    inline fun <reified T, reified R, reified E> resolver(argName1: String, argName2: String, noinline function: suspend (R, E) -> T) =
        resolver(FunctionWrapper.on(argName1, argName2, function))

    inline fun <reified T, reified R, reified E, reified W> resolver(argName1: String, argName2: String, argName3: String, noinline function: suspend (R, E, W) -> T) =
        resolver(FunctionWrapper.on(argName1, argName2, argName3, function))

    inline fun <reified T, reified R, reified E, reified W, reified Q> resolver(argName1: String, argName2: String, argName3: String, argName4: String, noinline function: suspend (R, E, W, Q) -> T) =
        resolver(FunctionWrapper.on(argName1, argName2, argName3, argName4, function))

    inline fun <reified T, reified R, reified E, reified W, reified Q, reified A> resolver(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, noinline function: suspend (R, E, W, Q, A) -> T) =
        resolver(FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, function))

    inline fun <reified T, reified R, reified E, reified W, reified Q, reified A, reified S> resolver(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, noinline function: suspend (R, E, W, Q, A, S) -> T) =
        resolver(FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, argName6, function))

    inline fun <reified T, reified R, reified E, reified W, reified Q, reified A, reified S, reified B> resolver(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, argName7: String, noinline function: suspend (R, E, W, Q, A, S, B) -> T) =
        resolver(FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, argName6, argName7, function))

    inline fun <reified T, reified R, reified E, reified W, reified Q, reified A, reified S, reified B, reified U> resolver(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, argName7: String, argName8: String, noinline function: suspend (R, E, W, Q, A, S, B, U) -> T) =
        resolver(FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, argName6, argName7, argName8, function))

    inline fun <reified T, reified R, reified E, reified W, reified Q, reified A, reified S, reified B, reified U, reified C> resolver(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, argName7: String, argName8: String, argName9: String, noinline function: suspend (R, E, W, Q, A, S, B, U, C) -> T) =
        resolver(FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, argName6, argName7, argName8, argName9, function))

    fun accessRule(rule: (Context) -> Exception?){
        val accessRuleAdapter: (Nothing?, Context) -> Exception? = { _, ctx -> rule(ctx) }
        this.accessRuleBlock = accessRuleAdapter
    }

    override fun addInputValues(inputValues: Collection<InputValueDef<*>>) {
        this.inputValues.addAll(inputValues)
    }

    override fun setReturnType(type: KType) {
        explicitReturnType = type
    }

}
