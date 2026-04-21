package com.ismartcoding.lib.kgraphql.schema.model

import kotlin.reflect.KType


interface OperationDef<T> : FunctionWrapper<T>, Depreciable, DescribedDef {

    val name : String

    override val argumentsDescriptor : Map<String, KType>
}