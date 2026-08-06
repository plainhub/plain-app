package com.ismartcoding.plain.lib.kgraphql.schema.model

import com.ismartcoding.plain.lib.kgraphql.schema.scalar.ScalarCoercion
import com.ismartcoding.plain.lib.kgraphql.schema.structure.Type
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
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
            val kotlinProperties: Map<KProperty1<T, *>, PropertyDef.Kotlin<T, *>> = emptyMap(),
            val extensionProperties : List<PropertyDef.Function<T, *>> = emptyList(),
            val dataloadExtensionProperties: List<PropertyDef.DataLoadedFunction<T, *, *>> = emptyList(),
            val unionProperties : List<PropertyDef.Union<T>> = emptyList(),
            val transformations : Map<KProperty1<T, *>, Transformation<T, *>> = emptyMap(),
            description : String? = null,
            // KSP-generated metadata — when set, SchemaCompilation uses these instead of
            // kotlin.reflect.full calls (isKotlinFinal, isKotlinSubclassOf, isKotlinSuperclassOf).
            // This enables iOS (Kotlin/Native) support without runtime reflection.
            val isInterface : Boolean = false,
            val possibleTypes : List<KClass<*>> = emptyList()
    ) : BaseKQLType(name, description), Kotlin<T> {

        val propertiesByName = kotlinProperties.mapKeys { entry -> entry.key.name }

        fun isIgnored(property: String): Boolean = propertiesByName[property]?.isIgnored ?: false
    }

    class Input<T : Any>(
            name : String,
            override val kClass: KClass<T>,
            // KSP bridge: when non-empty, SchemaCompilation uses these KProperty1 references
            // instead of memberPropertiesList() reflection. Enables iOS (Kotlin/Native).
            val kotlinProperties: List<KProperty1<T, *>> = emptyList(),
            // KSP bridge: return types keyed by property name — avoids KProperty1.returnType
            // reflection (kotlin.reflect.full) which is unavailable on iOS (Kotlin/Native).
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
