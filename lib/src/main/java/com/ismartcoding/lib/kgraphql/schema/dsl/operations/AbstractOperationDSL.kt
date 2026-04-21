package com.ismartcoding.lib.kgraphql.schema.dsl.operations

import com.ismartcoding.lib.kgraphql.Context
import com.ismartcoding.lib.kgraphql.schema.dsl.LimitedAccessItemDSL
import com.ismartcoding.lib.kgraphql.schema.dsl.ResolverDSL
import com.ismartcoding.lib.kgraphql.schema.model.FunctionWrapper
import com.ismartcoding.lib.kgraphql.schema.model.InputValueDef
import kotlin.reflect.KFunction
import kotlin.reflect.KType


abstract class AbstractOperationDSL(
    val name: String
) : LimitedAccessItemDSL<Nothing>(),
    ResolverDSL.Target {

    protected val inputValues = mutableListOf<InputValueDef<*>>()

    internal var functionWrapper: FunctionWrapper<*>? = null

    var explicitReturnType: KType? = null

    private fun resolver(function: FunctionWrapper<*>): ResolverDSL {

        try {
            require(function.hasReturnType()) {
                "Resolver for '$name' has no return value"
            }
        } catch (e: Throwable) {
            if ("KotlinReflectionInternalError" !in e.toString()) {
                throw e
            }
        }

        functionWrapper = function
        return ResolverDSL(this)
    }

    fun <T> KFunction<T>.toResolver() = resolver(FunctionWrapper.on(this))

    fun <T> resolver(function: suspend () -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R> resolver(function: suspend (R) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E> resolver(function: suspend (R, E) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W> resolver(function: suspend (R, E ,W ) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q> resolver(function: suspend (R, E, W, Q) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A> resolver(function: suspend (R, E, W, Q, A) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S> resolver(function: suspend (R, E, W, Q, A, S) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S, B> resolver(function: suspend (R, E, W, Q, A, S, B) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S, B, U> resolver(function: suspend (R, E, W, Q, A, S, B, U) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S, B, U, C> resolver(function: suspend (R, E, W, Q, A, S, B, U, C) -> T) = resolver(FunctionWrapper.on(function))

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
