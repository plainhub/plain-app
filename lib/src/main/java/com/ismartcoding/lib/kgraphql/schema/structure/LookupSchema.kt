package com.ismartcoding.lib.kgraphql.schema.structure

import com.ismartcoding.lib.kgraphql.isIterable
import com.ismartcoding.lib.kgraphql.request.TypeReference
import com.ismartcoding.lib.kgraphql.schema.Schema
import com.ismartcoding.lib.kgraphql.schema.introspection.TypeKind
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure


interface LookupSchema : Schema {

    fun typeByKClass(kClass: KClass<*>) : Type?

    fun typeByKType(kType: KType) : Type?

    fun typeByName(name: String) : Type?

    fun inputTypeByKClass(kClass: KClass<*>) : Type?

    fun inputTypeByKType(kType: KType) : Type?

    fun inputTypeByName(name: String) : Type?

    fun typeReference(kType: KType) : TypeReference {
        if(kType.jvmErasure.isIterable()){
            val elementKType = kType.arguments.first().type
                   ?: throw IllegalArgumentException("Cannot transform kotlin collection type $kType to KGraphQL TypeReference")
            val elementKTypeErasure = elementKType.jvmErasure

            val kqlType = typeByKClass(elementKTypeErasure) ?: inputTypeByKClass(elementKTypeErasure)
                    ?: throw IllegalArgumentException("$kType has not been registered in this schema")
            val name = kqlType.name ?: throw IllegalArgumentException("Cannot create type reference to unnamed type")

            return TypeReference(name, kType.isMarkedNullable, true, elementKType.isMarkedNullable)
        } else {
            val erasure = kType.jvmErasure
            val kqlType = typeByKClass(erasure) ?: inputTypeByKClass(erasure)
                    ?: throw IllegalArgumentException("$kType has not been registered in this schema")
            val name = kqlType.name ?: throw IllegalArgumentException("Cannot create type reference to unnamed type")

            return TypeReference(name, kType.isMarkedNullable)
        }
    }

    fun typeReference(type: Type) = TypeReference(
            name = type.unwrapped().name!!,
            isNullable = type.isNullable(),
            isList = type.isList(),
            isElementNullable = type.isList() && type.unwrapList().ofType?.kind == TypeKind.NON_NULL
    )
}
