package com.ismartcoding.plain.lib.kgraphql.schema.dsl.types

import com.ismartcoding.plain.lib.kgraphql.defaultKQLTypeName
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.ItemDSL
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType


class InputTypeDSL<T : Any>(val kClass: KClass<T>) : ItemDSL() {

    var name = kClass.defaultKQLTypeName()

    // KSP bridge: populated by registerGeneratedSchema() with ::foo references
    internal val declaredKotlinProperties = mutableListOf<KProperty1<T, *>>()

    // KSP bridge: return types keyed by property name — avoids KProperty1.returnType
    // reflection (kotlin.reflect.full) which is unavailable on iOS (Kotlin/Native).
    internal val declaredReturnTypes = mutableMapOf<String, KType>()

    fun property(kProperty: KProperty1<T, *>) {
        declaredKotlinProperties.add(kProperty)
    }

    /**
     * KSP bridge: register a property with a pre-computed return type.
     * The [returnType] is generated at compile time via `typeOf<R>()`,
     * avoiding runtime `kProperty.returnType` reflection (kotlin.reflect.full).
     */
    fun property(kProperty: KProperty1<T, *>, returnType: KType) {
        declaredKotlinProperties.add(kProperty)
        declaredReturnTypes[kProperty.name] = returnType
    }

    internal fun toKQLInput(): com.ismartcoding.plain.lib.kgraphql.schema.model.TypeDef.Input<T> {
        return com.ismartcoding.plain.lib.kgraphql.schema.model.TypeDef.Input(
            name = name,
            kClass = kClass,
            kotlinProperties = declaredKotlinProperties.toList(),
            returnTypes = declaredReturnTypes.toMap(),
            description = description
        )
    }
}
