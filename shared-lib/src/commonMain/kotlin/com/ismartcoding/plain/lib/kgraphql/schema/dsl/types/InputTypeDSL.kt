package com.ismartcoding.plain.lib.kgraphql.schema.dsl.types

import com.ismartcoding.plain.lib.kgraphql.defaultKQLTypeName
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.ItemDSL
import kotlin.reflect.KClass
import kotlin.reflect.KType

class InputTypeDSL<T : Any>(val kClass: KClass<T>) : ItemDSL() {

    var name = kClass.defaultKQLTypeName()
        set(value) {
            field = value
            nameCustomized = true
        }

    // True when the user set [name] explicitly in the DSL block; lets the KSP
    // merge override the (obfuscation-unsafe) simpleName default.
    internal var nameCustomized = false

    // Property names declared via DSL or @GraphQLInput descriptor.
    internal val declaredProperties = mutableListOf<String>()

    // Return types keyed by property name — KSP-generated via typeOf<R>().
    internal val declaredReturnTypes = mutableMapOf<String, KType>()

    fun property(name: String, returnType: KType) {
        declaredProperties.add(name)
        declaredReturnTypes[name] = returnType
    }

    internal fun toKQLInput(): com.ismartcoding.plain.lib.kgraphql.schema.model.TypeDef.Input<T> {
        return com.ismartcoding.plain.lib.kgraphql.schema.model.TypeDef.Input(
            name = name,
            kClass = kClass,
            properties = declaredProperties.toList(),
            returnTypes = declaredReturnTypes.toMap(),
            description = description
        )
    }
}
