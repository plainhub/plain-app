package com.ismartcoding.plain.lib.kgraphql

import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

/**
 * JVM actual for [createKType] — same hand-built implementation as iOS.
 *
 * `kotlin.reflect.full.createType` requires kotlin-reflect, whose metadata
 * parsing breaks under R8 obfuscation. The lightweight anonymous [KType]
 * below carries the [KClass] classifier and projection info — enough for
 * `KType.kClass()` and `KType.arguments` access used by VariablesJson.
 */
internal actual fun KClass<*>.createKType(args: List<KTypeProjection>, nullable: Boolean): KType {
    val owner = this
    return object : KType {
        override val classifier: KClassifier? = owner
        override val arguments: List<KTypeProjection> = args
        override val isMarkedNullable: Boolean = nullable
        override val annotations: List<Annotation> = emptyList()
    }
}
