package com.ismartcoding.lib.kgraphql.schema.dsl

import com.ismartcoding.lib.kgraphql.schema.dsl.types.InputValuesDSL
import com.ismartcoding.lib.kgraphql.schema.model.InputValueDef
import kotlin.reflect.*


class ResolverDSL(val target: Target) {
    fun withArgs(block : InputValuesDSL.() -> Unit){
        val inputValuesDSL = InputValuesDSL().apply(block)

        target.addInputValues(inputValuesDSL.inputValues.map { inputValue ->
            (inputValue.toKQLInputValue())
        })
    }

    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified T: Any> returns(): ResolverDSL {
        target.setReturnType(typeOf<T>())
        return this
    }

    interface Target {
        fun addInputValues(inputValues: Collection<InputValueDef<*>>)
        fun setReturnType(type: KType)
    }
}