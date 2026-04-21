package com.ismartcoding.lib.kgraphql.schema.structure

import com.ismartcoding.lib.kgraphql.Context
import com.ismartcoding.lib.kgraphql.schema.execution.Execution
import com.ismartcoding.lib.kgraphql.schema.introspection.TypeKind
import com.ismartcoding.lib.kgraphql.schema.introspection.__EnumValue
import com.ismartcoding.lib.kgraphql.schema.introspection.__Field
import com.ismartcoding.lib.kgraphql.schema.introspection.__InputValue
import com.ismartcoding.lib.kgraphql.schema.introspection.__Type
import com.ismartcoding.lib.kgraphql.schema.introspection.asString
import com.ismartcoding.lib.kgraphql.schema.model.TypeDef
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType


interface Type : __Type {

    fun hasField(name : String) : Boolean {
        return fields?.any { it.name == name } ?: false
    }

    operator fun get(name : String) : Field? = null

    fun unwrapped(): Type = when (kind) {
        TypeKind.NON_NULL, TypeKind.LIST -> (ofType as Type).unwrapped()
        else -> this
    }

    fun isNullable() = this.kind != TypeKind.NON_NULL

    fun isNotNullable() = this.kind == TypeKind.NON_NULL

    fun unwrapList() : Type = when(kind) {
        TypeKind.LIST -> ofType as Type
        else -> (ofType as Type?)?.unwrapList() ?: throw NoSuchElementException("this type does not wrap list element")
    }

    fun isList() : Boolean = when {
        kind == TypeKind.LIST -> true
        ofType == null -> false
        else -> (ofType as Type).isList()
    }

    fun isNotList() : Boolean = !isList()

    fun isElementNullable() = isList() && unwrapList().kind != TypeKind.NON_NULL

    fun isInstance(value : Any?) : Boolean = kClass?.isInstance(value) ?: false

    fun toKType(): KType {
        val unwrappedKClass : KClass<*> = unwrapped().kClass ?: throw IllegalArgumentException("This type cannot be represented as KType")

        return if(isList()){
            List::class.createType(listOf(KTypeProjection.covariant(unwrappedKClass.createType(nullable = isElementNullable()))), nullable = isNullable())
        } else {
            unwrappedKClass.createType(nullable = isNullable())
        }
    }

    val kClass : KClass<*>?

    abstract class ComplexType(val allFields: List<Field>) : Type {
        val fieldsByName = allFields.associateBy { it.name }

        override val fields: List<__Field>? = allFields.filterNot { it.name.startsWith("__") }

        override fun hasField(name: String): Boolean = fieldsByName[name] != null

        override fun get(name: String): Field? = fieldsByName[name]
    }

    class OperationObject (
            override val name: String,
            override val description: String,
            fields: List<Field>
    ) : ComplexType(fields){

        override val kClass: KClass<*>? = null

        override val kind: TypeKind = TypeKind.OBJECT

        override val enumValues: List<__EnumValue>? = null

        override val inputFields: List<__InputValue>? = null

        override val ofType: __Type? = null

        override val possibleTypes: List<__Type>? = null

        override val interfaces: List<Type.Interface<*>>? = emptyList()

        override fun isInstance(value: Any?): Boolean = false
    }

    class Object<T : Any>(
            private val definition: TypeDef.Object<T>,
            fields: List<Field> = emptyList(),
            override val interfaces: List<Type>? = emptyList()
    ) : ComplexType(fields) {

        override val kClass = definition.kClass

        override val kind: TypeKind = TypeKind.OBJECT

        override val name: String? = definition.name

        override val description: String = definition.description ?: ""

        override val enumValues: List<__EnumValue>? = null

        override val inputFields: List<__InputValue>? = null

        override val ofType: __Type? = null

        override val possibleTypes: List<__Type>? = null

        fun withInterfaces(interfaces : List<Type>) = Object(this.definition, this.allFields, interfaces)
    }

    class Interface<T : Any>(
            private val definition: TypeDef.Object<T>,
            fields: List<Field> = emptyList(),
            override val possibleTypes : List<Type>? = emptyList()
    ) : ComplexType(fields) {

        override val kClass = definition.kClass

        override val kind: TypeKind = TypeKind.INTERFACE

        override val name: String? = definition.name

        override val description: String = definition.description ?: ""

        override val enumValues: List<__EnumValue>? = null

        override val inputFields: List<__InputValue>? = null

        override val ofType: __Type? = null

        override val interfaces: List<__Type>? = null

        fun withPossibleTypes(possibleTypes: List<Type>) = Interface(this.definition, this.allFields, possibleTypes)
    }

    class Scalar<T : Any>(
            kqlType : TypeDef.Scalar<T>
    ) : Type {

        override val kClass = kqlType.kClass

        override val kind: TypeKind = TypeKind.SCALAR

        override val name: String? = kqlType.name

        override val description: String = kqlType.description ?: ""

        override val enumValues: List<__EnumValue>? = null

        override val inputFields: List<__InputValue>? = null

        override val ofType: __Type? = null

        override val interfaces: List<__Type>? = null

        override val fields: List<__Field>? = null

        override val possibleTypes: List<__Type>? = null

        val coercion = kqlType.coercion
    }

    class Enum<T : kotlin.Enum<T>>(
            kqlType: TypeDef.Enumeration<T>
    ) : Type {

        val values = kqlType.values.map { it.toEnumValue() }

        override val kClass = kqlType.kClass

        override val kind: TypeKind = TypeKind.ENUM

        override val name: String? = kqlType.name

        override val description: String = kqlType.description ?: ""

        override val enumValues: List<__EnumValue>? = values

        override val inputFields: List<__InputValue>? = null

        override val ofType: __Type? = null

        override val interfaces: List<__Type>? = null

        override val fields: List<__Field>? = null

        override val possibleTypes: List<__Type>? = null
    }

    class Input<T : Any>(
            kqlType: TypeDef.Input<T>,
            override val inputFields: List<InputValue<*>> = emptyList()
    ) : Type {

        override val kClass = kqlType.kClass

        override val kind: TypeKind = TypeKind.INPUT_OBJECT

        override val name: String? = kqlType.name

        override val description: String = kqlType.description ?: ""

        override val enumValues: List<__EnumValue>? = null

        override val ofType: __Type? = null

        override val interfaces: List<__Type>? = null

        override val fields: List<__Field>? = null

        override val possibleTypes: List<__Type>? = null
    }

    class Union(
            kqlType: TypeDef.Union,
            typenameResolver: Field,
            override val possibleTypes: List<Type>
    ) : ComplexType(listOf(typenameResolver)) {
        override val kClass: KClass<*>? = null

        override val kind: TypeKind = TypeKind.UNION

        override val name: String? = kqlType.name

        override val description: String = kqlType.description ?: ""

        override val enumValues: List<__EnumValue>? = null

        override val inputFields: List<__InputValue>? = null

        override val ofType: __Type? = null

        override val interfaces: List<__Type>? = null

        override val fields: List<__Field>? = null

        override fun isInstance(value: Any?): Boolean = false
    }

    class _Context : Type {
        override val kClass: KClass<*>? = Context::class

        override val kind: TypeKind = TypeKind.OBJECT

        override val name: String? = null

        override val description: String = ""

        override val enumValues: List<__EnumValue>? = null

        override val inputFields: List<__InputValue>? = null

        override val ofType: __Type? = null

        override val interfaces: List<__Type>? = null

        override val fields: List<__Field>? = null

        override val possibleTypes: List<__Type>? = null
    }

    class _ExecutionNode : Type {
        override val kClass: KClass<*>? = Execution.Node::class

        override val kind: TypeKind = TypeKind.OBJECT

        override val name: String? = null

        override val description: String = ""

        override val enumValues: List<__EnumValue>? = null

        override val inputFields: List<__InputValue>? = null

        override val ofType: __Type? = null

        override val interfaces: List<__Type>? = null

        override val fields: List<__Field>? = null

        override val possibleTypes: List<__Type>? = null
    }

    class NonNull(override val ofType: Type) : Type {

        override val kClass: KClass<*>? = null

        override val kind: TypeKind = TypeKind.NON_NULL

        override val name: String? = null

        override val description: String = "NonNull wrapper type"

        override val fields: List<__Field>? = null

        override val interfaces: List<__Type>? = null

        override val possibleTypes: List<__Type>? = null

        override val enumValues: List<__EnumValue>? = null

        override val inputFields: List<__InputValue>? = null

        override fun toString(): String = asString()

        override fun isInstance(value: Any?): Boolean = false
    }

    class AList(val elementType: Type) : Type {

        override val kClass: KClass<*>? = null

        override val ofType: __Type? = elementType

        override val kind: TypeKind = TypeKind.LIST

        override val name: String? = null

        override val description: String = "List wrapper type"

        override val fields: List<__Field>? = null

        override val interfaces: List<__Type>? = null

        override val possibleTypes: List<__Type>? = null

        override val enumValues: List<__EnumValue>? = null

        override val inputFields: List<__InputValue>? = null

        override fun toString() = asString()

        override fun isInstance(value: Any?): Boolean = false
    }
}
