package com.ismartcoding.plain.lib.kgraphql

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

// iOS stubs — GraphQL is server-side (Android) only.
// These throw UnsupportedOperationException if ever called on iOS.

private fun unsupported(): Nothing = throw UnsupportedOperationException("kgraphql reflection is only supported on Android (server-side)")

internal actual fun <T : Any> KClass<T>.memberPropertiesList(): List<KProperty1<T, *>> = unsupported()

internal actual fun <T, R> KProperty1<T, R>.isPublicVisibility(): Boolean = unsupported()

internal actual fun KClass<*>.isKotlinSubclassOf(base: KClass<*>): Boolean = unsupported()

internal actual fun KClass<*>.isKotlinSuperclassOf(subclass: KClass<*>): Boolean = unsupported()

@PublishedApi
internal actual fun KClass<*>.isKotlinSealed(): Boolean = unsupported()

internal actual fun KClass<*>.isKotlinFinal(): Boolean = unsupported()

internal actual fun KClass<*>.hasNotIntrospectedAnnotation(): Boolean = unsupported()

internal actual fun <T : Any> KClass<T>.callConstructorBy(args: Map<String, Any?>): T = unsupported()

internal actual fun <T : Any> KClass<T>.getConstructorParamNames(): List<String> = unsupported()

internal actual fun <T : Any> KClass<T>.isConstructorParamOptional(name: String): Boolean = unsupported()

internal actual fun KType.isKotlinSubtypeOf(other: KType): Boolean = unsupported()

internal actual fun <T : Any> KClass<T>.starProjectedKType(): KType = unsupported()

internal actual fun <T : Any> getFieldValueByName(obj: T, fieldName: String): Any? = unsupported()

internal actual fun KClass<*>.isKotlinEnum(): Boolean = unsupported()

internal actual fun enumValueOfSafe(kClass: KClass<*>, name: String): Any? = unsupported()

internal actual fun KClass<*>.createKType(args: List<KTypeProjection>, nullable: Boolean): KType = unsupported()

@PublishedApi
internal actual fun <T : Any> KClass<T>.sealedSubclassesList(): List<KClass<out T>> = unsupported()
