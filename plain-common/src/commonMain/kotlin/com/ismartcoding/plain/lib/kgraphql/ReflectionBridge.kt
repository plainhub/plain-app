package com.ismartcoding.plain.lib.kgraphql

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

/**
 * Bridge for the single remaining KType materialization need: `Type.toKType()`
 * uses it for `VariablesJson` variable resolution. All reflection
 * (memberProperties, isSubclassOf, isSealed, callConstructor, createType,
 * KProperty1 handles) has been replaced by KSP-generated descriptors and
 * accessor lambdas on every platform — including the JVM.
 *
 * Both actuals construct a lightweight anonymous [KType] that captures the
 * [KClass] classifier and projection info — sufficient for `KType.kClass()`
 * and `KType.arguments` access used by VariablesJson.
 */
internal expect fun KClass<*>.createKType(args: List<KTypeProjection>, nullable: Boolean): KType
