package com.ismartcoding.plain.lib.kgraphql

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

/**
 * Bridge for JVM-only reflection operations (kotlin.reflect.full).
 *
 * After the KSP2 migration, the only reflection operation still required at
 * runtime is [createKType] — used by `Type.toKType()` to materialize a [KType]
 * for `VariablesJson` variable resolution. All other reflection
 * (`memberProperties`, `isSubclassOf`, `isSealed`, `callConstructor`, etc.)
 * has been replaced by KSP-generated descriptors.
 *
 * On Android, [createKType] delegates to `kotlin.reflect.full.createType`.
 * On iOS, [createKType] constructs a lightweight anonymous [KType] that
 * captures the [KClass] classifier and projection info — sufficient for
 * `KType.kClass()` and `KType.arguments` access used by VariablesJson.
 */
internal expect fun KClass<*>.createKType(args: List<KTypeProjection>, nullable: Boolean): KType
