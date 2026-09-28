package com.ismartcoding.plain.lib.kgraphql.schema.model

data class Transformation<T : Any, R>(
        val name: String,
        val transformation : FunctionWrapper<R>
) : FunctionWrapper<R> by transformation
