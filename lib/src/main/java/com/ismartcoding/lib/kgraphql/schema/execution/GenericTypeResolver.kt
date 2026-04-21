package com.ismartcoding.lib.kgraphql.schema.execution

import com.ismartcoding.lib.kgraphql.schema.SchemaException
import java.util.*
import kotlin.reflect.KType

/**
 * A generic type resolver takes values that are wrapped in classes like {@link java.util.Optional} / {@link java.util.OptionalInt} etc..
 * and returns value from them.  You can provide your own implementation if you have your own specific
 * holder classes.
 */
interface GenericTypeResolver {

    fun unbox(obj: Any): Any?

    fun resolveMonad(type: KType): KType

    companion object {
        val DEFAULT = DefaultGenericTypeResolver()
    }
}

open class DefaultGenericTypeResolver : GenericTypeResolver {

    override fun unbox(obj: Any): Any? = obj

    override fun resolveMonad(type: KType): KType =
        throw SchemaException("Could not resolve resulting type for monad $type. " +
                "Please provide custom GenericTypeResolver to KGraphQL configuration to register your generic types")
}