package com.ismartcoding.lib.kgraphql.schema.dsl.types

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf


class InputValuesDSL {

    val inputValues = mutableListOf<InputValueDSL<*>>()

    fun <T : Any> arg(kClass: KClass<T>, kType: KType? = null, block: InputValueDSL<T>.() -> Unit) {
        inputValues.add(InputValueDSL(kClass, kType).apply(block))
    }

    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified T : Any> arg(optional: Boolean = false, noinline block: InputValueDSL<T>.() -> Unit) {
        val kType = if (optional) typeOf<T?>() else typeOf<T>()
        arg(T::class, kType, block)
    }
}
