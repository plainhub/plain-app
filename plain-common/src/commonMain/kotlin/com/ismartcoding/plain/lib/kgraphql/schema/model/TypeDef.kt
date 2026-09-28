package com.ismartcoding.plain.lib.kgraphql.schema.model

import com.ismartcoding.plain.lib.kgraphql.schema.scalar.ScalarCoercion
import com.ismartcoding.plain.lib.kgraphql.schema.structure.Type
import kotlin.reflect.KClass
import kotlin.reflect.KType

interface TypeDef {

    val name : String

    val description : String?

    abstract class BaseKQLType(name : String, override val description: String?) : TypeDef, Definition(name)

    interface Kotlin<T : Any> : TypeDef {
        val kClass : KClass<T>
    }

    class Object<T : Any> (
            name : String,
            override val kClass: KClass<T>,
            // Property name -> property definition. KSP-generated accessors replace
            // KProperty1 handles — reflection-free on every platform.
            val kotlinProperties: Map<String, PropertyDef.Kotlin<T, *>> = emptyMap(),
            val extensionProperties : List<PropertyDef.Function<T, *>> = emptyList(),
            val dataloadExtensionProperties: List<PropertyDef.DataLoadedFunction<T, *, *>> = emptyList(),
            val unionProperties : List<PropertyDef.Union<T>> = emptyList(),
            val transformations : Map<String, Transformation<T, *>> = emptyMap(),
            description : String? = null,
            // KSP-generated metadata — when set, SchemaCompilation uses these instead of
            // kotlin.reflect.full calls (isKotlinFinal, isKotlinSubclassOf, isKotlinSuperclassOf).
            // This enables iOS (Kotlin/Native) support without runtime reflection.
            val isInterface : Boolean = false,
            val possibleTypes : List<KClass<*>> = emptyList()
    ) : BaseKQLType(name, description), Kotlin<T> {

        fun isIgnored(property: String): Boolean = kotlinProperties[property]?.isIgnored ?: false
    }

    class Input<T : Any>(
            name : String,
            override val kClass: KClass<T>,
            // Property names declared via @GraphQLInput descriptor or DSL.
            val properties: List<String> = emptyList(),
            // Return types keyed by property name — KSP-generated via typeOf<R>().
            val returnTypes: Map<String, KType> = emptyMap(),
            description: String? = null
    ) : BaseKQLType(name, description), Kotlin<T>

    class Scalar<T : Any> (
        name : String,
        override val kClass: KClass<T>,
        val coercion: ScalarCoercion<T, *>,
        description : String?
    ) : BaseKQLType(name, description), Kotlin<T> {
        fun toScalarType() : Type.Scalar<T> = Type.Scalar(this)
    }

    //To avoid circular dependencies etc. union type members are resolved in runtime
    class Union (
            name : String,
            val members: Set<KClass<*>>,
            description : String?
    ) : BaseKQLType(name, description)

    class Enumeration<T : Enum<T>> (
            name: String,
            override val kClass: KClass<T>,
            val values: List<EnumValueDef<T>>,
            description : String? = null
    ) : BaseKQLType(name, description), Kotlin<T> {

        fun toEnumType() : Type.Enum<T> = Type.Enum(this)
    }
}
