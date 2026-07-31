package com.ismartcoding.plain.lib.kgraphql

import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

/**
 * iOS actual for [createKType].
 *
 * Kotlin/Native does not expose `kotlin.reflect.full.createType`, but [KType]
 * is just an interface. We construct an anonymous [KType] that carries the
 * [KClass] classifier and projection info — enough for `KType.kClass()`
 * (a simple `classifier as? KClass<*>` cast) and `KType.arguments` access
 * used by VariablesJson for variable resolution.
 *
 * This is only reached when executing GraphQL queries with variables on iOS.
 * Schema compilation uses KSP-generated descriptors and never calls this.
 */
internal actual fun KClass<*>.createKType(args: List<KTypeProjection>, nullable: Boolean): KType {
    val owner = this
    return object : KType {
        override val classifier: KClassifier? = owner
        override val arguments: List<KTypeProjection> = args
        override val isMarkedNullable: Boolean = nullable
    }
}
