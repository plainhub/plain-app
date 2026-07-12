package com.ismartcoding.plain.lib.kgraphql

import com.ismartcoding.plain.lib.kgraphql.schema.introspection.NotIntrospected
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVisibility
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType

internal actual fun <T : Any> KClass<T>.memberPropertiesList(): List<KProperty1<T, *>> = memberProperties.toList()

internal actual fun <T, R> KProperty1<T, R>.isPublicVisibility(): Boolean = visibility == KVisibility.PUBLIC

internal actual fun KClass<*>.isKotlinSubclassOf(base: KClass<*>): Boolean = isSubclassOf(base)

internal actual fun KClass<*>.isKotlinSuperclassOf(subclass: KClass<*>): Boolean = isSuperclassOf(subclass)

@PublishedApi
internal actual fun KClass<*>.isKotlinSealed(): Boolean = isSealed

internal actual fun KClass<*>.isKotlinFinal(): Boolean = isFinal

internal actual fun KClass<*>.hasNotIntrospectedAnnotation(): Boolean = findAnnotation<NotIntrospected>() != null

internal actual fun <T : Any> KClass<T>.callConstructorBy(args: Map<String, Any?>): T {
    val constructor = primaryConstructor ?: throw IllegalStateException("No primary constructor for $this")
    val params = constructor.parameters.associateBy { it.name }
    val valueMap = args.mapKeys { (name, _) -> params.getValue(name) }
    return constructor.callBy(valueMap)
}

internal actual fun <T : Any> KClass<T>.getConstructorParamNames(): List<String> {
    val constructor = primaryConstructor ?: throw IllegalStateException("No primary constructor for $this")
    return constructor.parameters.mapNotNull { it.name }
}

internal actual fun <T : Any> KClass<T>.isConstructorParamOptional(name: String): Boolean {
    val constructor = primaryConstructor ?: throw IllegalStateException("No primary constructor for $this")
    val param = constructor.parameters.find { it.name == name } ?: return false
    return param.isOptional
}

internal actual fun KType.isKotlinSubtypeOf(other: KType): Boolean = isSubtypeOf(other)

internal actual fun <T : Any> KClass<T>.starProjectedKType(): KType = starProjectedType

internal actual fun <T : Any> getFieldValueByName(obj: T, fieldName: String): Any? {
    val properties = obj::class.memberProperties
    for (p in properties) {
        if (p.name == fieldName) {
            @Suppress("UNCHECKED_CAST")
            return (p as KProperty1<T, *>).getter.call(obj)
        }
    }
    return null
}

internal actual fun KClass<*>.isKotlinEnum(): Boolean = java.isEnum

internal actual fun enumValueOfSafe(kClass: KClass<*>, name: String): Any? {
    val constants = kClass.java.enumConstants as? Array<out Enum<*>>
        ?: throw IllegalArgumentException("No enum constants for $kClass")
    for (constant in constants) {
        if (constant.name == name) {
            return constant
        }
    }
    throw IllegalArgumentException("No enum constant ${kClass.qualifiedName}.$name")
}

internal actual fun KClass<*>.createKType(args: List<KTypeProjection>, nullable: Boolean): KType = createType(args, nullable = nullable)

@PublishedApi
internal actual fun <T : Any> KClass<T>.sealedSubclassesList(): List<KClass<out T>> = sealedSubclasses
