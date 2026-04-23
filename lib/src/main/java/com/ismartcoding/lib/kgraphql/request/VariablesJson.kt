package com.ismartcoding.lib.kgraphql.request

import com.ismartcoding.lib.kgraphql.ExecutionException
import com.ismartcoding.lib.kgraphql.GraphQLError
import com.ismartcoding.lib.kgraphql.getIterableElementType
import com.ismartcoding.lib.kgraphql.isIterable
import com.ismartcoding.lib.kgraphql.schema.model.ast.NameNode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.long
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

    class Defined(
        val json: JsonObject,
        private val scalarDeserializers: Map<KClass<*>, (JsonElement) -> Any?> = emptyMap()
    ) : VariablesJson {

        constructor(json: String, scalarDeserializers: Map<KClass<*>, (JsonElement) -> Any?> = emptyMap()) :
            this(Json.parseToJsonElement(json).jsonObject, scalarDeserializers)

        override fun <T : Any> get(kClass: KClass<T>, kType: KType, key: NameNode): T? {
            require(kClass == kType.jvmErasure) { "kClass and KType must represent same class" }
            return json[key.value]?.let { element ->
                try {
                    convertElement(element, kClass, kType)
                } catch (e: Exception) {
                    throw if (e is GraphQLError) e
                    else ExecutionException("Failed to coerce $element as $kType", key, e)
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : Any> convertElement(element: JsonElement, kClass: KClass<T>, kType: KType): T? {
            if (element is JsonNull) return null
            return when {
                kClass == String::class -> (element as JsonPrimitive).content as T
                kClass == Int::class -> (element as JsonPrimitive).int as T
                kClass == Long::class -> (element as JsonPrimitive).long as T
                kClass == Double::class -> (element as JsonPrimitive).double as T
                kClass == Float::class -> (element as JsonPrimitive).float as T
                kClass == Short::class -> (element as JsonPrimitive).int.toShort() as T
                kClass == Boolean::class -> (element as JsonPrimitive).boolean as T
                kClass.isIterable() -> {
                    val elementKType = kType.getIterableElementType()
                        ?: throw ExecutionException("Cannot handle collection without element type")
                    val elementKClass = elementKType.jvmErasure
                    (element as JsonArray).map { convertElement(it, elementKClass, elementKType) } as T
                }
                kClass.java.isEnum -> {
                    val content = (element as JsonPrimitive).content
                    @Suppress("UNCHECKED_CAST")
                    java.lang.Enum.valueOf(kClass.java as Class<out Enum<*>>, content) as T
                }
                else -> {
                    val deserializer = scalarDeserializers[kClass]
                        ?: throw ExecutionException("No deserializer registered for type $kClass")
                    deserializer(element) as T
                }
            }
        }
    }
}
