package com.ismartcoding.lib.kgraphql

import com.ismartcoding.lib.kgraphql.schema.introspection.NotIntrospected
import kotlin.reflect.KClass

@NotIntrospected
class Context(private val map: Map<Any, Any>) {

    operator fun <T : Any> get(kClass: KClass<T>): T? {
        val value = map[kClass]
        @Suppress("UNCHECKED_CAST")
        return  if(kClass.isInstance(value)) value as T else null
    }

    inline fun <reified T : Any> get() : T? = get(T::class)

}