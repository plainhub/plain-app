package com.ismartcoding.lib.kgraphql.schema.model

import kotlin.reflect.KProperty1


data class Transformation<T : Any, R>(
        val kProperty: KProperty1<T, R>,
        val transformation : FunctionWrapper<R>
) : FunctionWrapper<R> by transformation