package com.ismartcoding.lib.kgraphql.schema.model

import com.ismartcoding.lib.kgraphql.Context
import kotlin.reflect.KType

abstract class BaseOperationDef<T, R>(
        name : String,
        private val operationWrapper: FunctionWrapper<R>,
        val inputValues : List<InputValueDef<*>>,
        val accessRule : ((T?, Context) -> Exception?)?,
        private val explicitReturnType: KType?
) : Definition(name), OperationDef<R>, FunctionWrapper<R> by operationWrapper {

        val returnType: KType get() = explicitReturnType ?: kFunction.returnType

}