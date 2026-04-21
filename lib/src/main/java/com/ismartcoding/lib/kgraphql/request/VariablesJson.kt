package com.ismartcoding.lib.kgraphql.request

import com.ismartcoding.lib.kgraphql.ExecutionException
import com.ismartcoding.lib.kgraphql.GraphQLError
import com.ismartcoding.lib.kgraphql.getIterableElementType
import com.ismartcoding.lib.kgraphql.isIterable
import com.ismartcoding.lib.kgraphql.schema.model.ast.NameNode
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

/**
 * Represents already parsed variables json
 */
interface VariablesJson {

    fun <T : Any> get(kClass: KClass<T>, kType: KType, key : NameNode) : T?

    class Empty : VariablesJson {
        override fun <T : Any> get(kClass: KClass<T>, kType: KType, key: NameNode): T? {
            return null
        }
    }

    class Defined(val objectMapper: ObjectMapper, val json: JsonNode) : VariablesJson {

        constructor(objectMapper: ObjectMapper, json : String) : this(objectMapper, objectMapper.readTree(json))

        /**
         * map and return object of requested class
         */
        override fun <T : Any>get(kClass: KClass<T>, kType: KType, key : NameNode) : T? {
            require(kClass == kType.jvmErasure) { "kClass and KType must represent same class" }
            return json.let { node -> node[key.value] }?.let { tree ->
                try {
                    // TODO: Move away from jackson and only depend on kotlinx.serialization
                    objectMapper.treeToValue<T>(tree, kType.toTypeReference())
                } catch(e : Exception) {
                    throw if (e is GraphQLError) e
                    else ExecutionException("Failed to coerce $tree as $kType", key, e)
                }
            }
        }
    }

    fun KType.toTypeReference(): JavaType {
        return if(jvmErasure.isIterable()) {
            val elementType = getIterableElementType()
                ?: throw ExecutionException("Cannot handle collection without element type")

            TypeFactory.defaultInstance().constructCollectionType(List::class.java, elementType.jvmErasure.java)
        } else {
            TypeFactory.defaultInstance().constructSimpleType(jvmErasure.java, emptyArray())
        }
    }
}
