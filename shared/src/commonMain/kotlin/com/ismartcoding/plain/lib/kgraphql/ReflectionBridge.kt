package com.ismartcoding.plain.lib.kgraphql

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

/**
 * Bridge for JVM-only reflection operations (kotlin.reflect.full).
 * On Android, these delegate to kotlin.reflect.full.
 * On iOS, these throw UnsupportedOperationException as GraphQL is server-side only.
 *
 * NOTE: KProperty1.returnType and KProperty1.name ARE available in common Kotlin
 * (declared in KCallable interface), so they don't need bridge functions.
 * Only operations from kotlin.reflect.full (memberProperties, isSubclassOf, etc.)
 * and java.* APIs need bridging.
 */
internal expect fun <T : Any> KClass<T>.memberPropertiesList(): List<KProperty1<T, *>>
internal expect fun <T, R> KProperty1<T, R>.isPublicVisibility(): Boolean
internal expect fun KClass<*>.isKotlinSubclassOf(base: KClass<*>): Boolean
internal expect fun KClass<*>.isKotlinSuperclassOf(subclass: KClass<*>): Boolean
@PublishedApi
internal expect fun KClass<*>.isKotlinSealed(): Boolean
internal expect fun KClass<*>.isKotlinFinal(): Boolean
internal expect fun KClass<*>.hasNotIntrospectedAnnotation(): Boolean
internal expect fun <T : Any> KClass<T>.callConstructorBy(args: Map<String, Any?>): T
internal expect fun <T : Any> KClass<T>.getConstructorParamNames(): List<String>
internal expect fun <T : Any> KClass<T>.isConstructorParamOptional(name: String): Boolean
internal expect fun KType.isKotlinSubtypeOf(other: KType): Boolean
internal expect fun <T : Any> KClass<T>.starProjectedKType(): KType
internal expect fun <T : Any> getFieldValueByName(obj: T, fieldName: String): Any?
internal expect fun KClass<*>.isKotlinEnum(): Boolean
internal expect fun enumValueOfSafe(kClass: KClass<*>, name: String): Any?
internal expect fun KClass<*>.createKType(args: List<KTypeProjection>, nullable: Boolean): KType
@PublishedApi
internal expect fun <T : Any> KClass<T>.sealedSubclassesList(): List<KClass<out T>>
