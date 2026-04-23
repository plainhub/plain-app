package com.ismartcoding.lib.kgraphql.schema.model

import com.ismartcoding.lib.kgraphql.Context
import com.ismartcoding.lib.kdataloader.factories.DataLoaderFactory
import kotlin.reflect.KProperty1
import kotlin.reflect.KType

interface PropertyDef<T> : Depreciable, DescribedDef {

    val accessRule : ((T?, Context) -> Exception?)?

    val name : String

    open class Function<T, R> (
            name : String,
            resolver: FunctionWrapper<R>,
            override val description: String? = null,
            override val isDeprecated: Boolean = false,
            override val deprecationReason: String? = null,
            accessRule : ((T?, Context) -> Exception?)? = null,
            inputValues : List<InputValueDef<*>> = emptyList(),
            explicitReturnType: KType? = null
    ) : BaseOperationDef<T, R>(name, resolver, inputValues, accessRule, explicitReturnType), PropertyDef<T>

    /**
     * [T] -> The Parent Type
     * [K] -> The Key that'll be passed to the dataLoader
     * [R] -> The return type
     */
    open class DataLoadedFunction<T, K, R>(
        override val name: String,
        val loader: DataLoaderFactory<K, R>,
        val prepare: FunctionWrapper<K>,
        val returnType: KType,
        override val description: String? = null,
        override val isDeprecated: Boolean = false,
        override val deprecationReason: String? = null,
        override val accessRule: ((T?, Context) -> Exception?)? = null,
        val inputValues: List<InputValueDef<*>> = emptyList()
    ): PropertyDef<T>

    open class Kotlin<T : Any, R> (
            val kProperty: KProperty1<T, R>,
            override val description: String? = null,
            override val isDeprecated: Boolean = false,
            override val deprecationReason: String? = null,
            override val accessRule : ((T?, Context) -> Exception?)? = null,
            val isIgnored : Boolean = false
    ) : Definition(kProperty.name), PropertyDef<T>

    class Union<T> (
            name : String,
            resolver : FunctionWrapper<Any?>,
            val union : TypeDef.Union,
            description: String?,
            val nullable: Boolean,
            isDeprecated: Boolean,
            deprecationReason: String?,
            accessRule : ((T?, Context) -> Exception?)? = null,
            inputValues : List<InputValueDef<*>>
    ) : Function<T, Any?>(name, resolver, description, isDeprecated, deprecationReason, accessRule, inputValues), PropertyDef<T>
}
