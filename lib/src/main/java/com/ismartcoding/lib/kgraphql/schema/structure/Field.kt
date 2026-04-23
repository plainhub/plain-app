package com.ismartcoding.lib.kgraphql.schema.structure

import com.ismartcoding.lib.kgraphql.Context
import com.ismartcoding.lib.kgraphql.schema.introspection.*
import com.ismartcoding.lib.kgraphql.schema.model.*
import com.ismartcoding.lib.kdataloader.factories.DataLoaderFactory
import kotlin.reflect.full.findAnnotation


sealed class Field : __Field {

    abstract val arguments : List<InputValue<*>>

    override val args: List<__InputValue>
        get() = arguments.filterNot { it.type.kClass?.findAnnotation<NotIntrospected>() != null }

    abstract val returnType : Type

    override val type: __Type
        get() = returnType

    abstract fun checkAccess(parent : Any?, ctx: Context)

    open class Function<T, R>(
            kql : BaseOperationDef<T, R>,
            override val returnType: Type,
            override val arguments: List<InputValue<*>>
    ) : Field(), FunctionWrapper<R> by kql {

        override val name: String = kql.name

        override val description: String? = kql.description

        override val isDeprecated: Boolean = kql.isDeprecated

        override val deprecationReason: String? = kql.deprecationReason

        val accessRule : ((T?, Context) -> Exception?)? = kql.accessRule

        override fun checkAccess(parent: Any?, ctx: Context) {
            accessRule?.invoke(parent as T?, ctx)?.let { throw it }
        }
    }

    class DataLoader<T, K, R>(
        val kql: PropertyDef.DataLoadedFunction<T, K, R>,
        val loader: DataLoaderFactory<K, R>,
        override val returnType: Type,
        override val arguments: List<InputValue<*>>
    ): Field() {
        override val isDeprecated = kql.isDeprecated
        override val deprecationReason = kql.deprecationReason
        override val description = kql.description
        override val name = kql.name

        override fun checkAccess(parent: Any?, ctx: Context) {
            kql.accessRule?.invoke(parent as T?, ctx)?.let { throw it }
        }
    }

    class Kotlin<T : Any, R>(
        kql : PropertyDef.Kotlin<T, R>,
        override val returnType: Type,
        override val arguments: List<InputValue<*>>,
        val transformation : Transformation<T, R>?
    ) : Field(){

        val kProperty = kql.kProperty

        override val name: String = kql.name

        override val description: String? = kql.description

        override val isDeprecated: Boolean = kql.isDeprecated

        override val deprecationReason: String? = kql.deprecationReason

        val accessRule : ((T?, Context) -> Exception?)? = kql.accessRule

        override fun checkAccess(parent: Any?, ctx: Context) {
            accessRule?.invoke(parent as T?, ctx)?.let { throw it }
        }
    }

    class Union<T> (
            kql : PropertyDef.Union<T>,
            val nullable: Boolean,
            override val returnType: Type.Union,
            override val arguments: List<InputValue<*>>
    ) : Field(), FunctionWrapper<Any?> by kql {

        override val name: String = kql.name

        override val description: String? = kql.description

        override val isDeprecated: Boolean = kql.isDeprecated

        override val deprecationReason: String? = kql.deprecationReason

        val accessRule : ((T?, Context) -> Exception?)? = kql.accessRule

        override fun checkAccess(parent: Any?, ctx: Context) {
            accessRule?.invoke(parent as T?, ctx)?.let { throw it }
        }
    }
}
