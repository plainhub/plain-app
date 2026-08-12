package com.ismartcoding.plain.lib.kgraphql.schema.model

import com.ismartcoding.plain.lib.kgraphql.defaultKQLTypeName
import com.ismartcoding.plain.lib.kgraphql.schema.SchemaException
import com.ismartcoding.plain.lib.kgraphql.schema.builtin.BUILT_IN_TYPE
import com.ismartcoding.plain.lib.kgraphql.schema.directive.Directive
import com.ismartcoding.plain.lib.kgraphql.schema.directive.DirectiveLocation
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.types.TypeDSL
import com.ismartcoding.plain.lib.kgraphql.schema.introspection.*
import com.ismartcoding.plain.lib.kgraphql.schema.introspection.TypeKind
import com.ismartcoding.plain.lib.kgraphql.schema.introspection.__Directive
import com.ismartcoding.plain.lib.kgraphql.schema.introspection.__EnumValue
import com.ismartcoding.plain.lib.kgraphql.schema.introspection.__Field
import com.ismartcoding.plain.lib.kgraphql.schema.introspection.__InputValue
import com.ismartcoding.plain.lib.kgraphql.schema.introspection.__Schema
import com.ismartcoding.plain.lib.kgraphql.schema.introspection.__Type
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

/**
 * Intermediate, mutable data structure used to prepare [SchemaDefinition]
 * Performs basic validation (names duplication etc.) when methods for adding schema components are invoked
 */
data class MutableSchemaDefinition (
    private val objects: ArrayList<TypeDef.Object<*>> = arrayListOf(
        create__SchemaDefinition(),
        create__TypeDefinition(),
        create__DirectiveDefinition(),
        create__FieldDefinition(),
        create__InputValueDefinition(),
        create__EnumValueDefinition()
    ),
    private val queries: ArrayList<QueryDef<*>> = arrayListOf(),
    private val scalars: ArrayList<TypeDef.Scalar<*>> = arrayListOf(
        BUILT_IN_TYPE.STRING,
        BUILT_IN_TYPE.BOOLEAN,
        BUILT_IN_TYPE.DOUBLE,
        BUILT_IN_TYPE.FLOAT,
        BUILT_IN_TYPE.SHORT,
        BUILT_IN_TYPE.INT,
        BUILT_IN_TYPE.LONG,

    ),
    private val mutations: ArrayList<MutationDef<*>> = arrayListOf(),
    private val subscriptions: ArrayList<SubscriptionDef<*>> = arrayListOf(),
        private val enums: ArrayList<TypeDef.Enumeration<*>> = arrayListOf(
        TypeDef.Enumeration(
            "__" + TypeKind::class.defaultKQLTypeName(),
            TypeKind::class,
            enumValues<TypeKind>().map { EnumValueDef(it) }
        ),
        TypeDef.Enumeration(
            "__" + DirectiveLocation::class.defaultKQLTypeName(),
            DirectiveLocation::class,
            enumValues<DirectiveLocation>().map { EnumValueDef(it) }
        )
    ),
    private val unions: ArrayList<TypeDef.Union> = arrayListOf(),
    private val directives: ArrayList<Directive.Partial> = arrayListOf(
        Directive.Companion.SKIP,
        Directive.Companion.INCLUDE
    ),
    private val inputObjects: ArrayList<TypeDef.Input<*>> = arrayListOf()
) {

    val unionsMonitor : List<TypeDef.Union>
        get() = unions

    fun toSchemaDefinition() : SchemaDefinition {
        val compiledObjects = ArrayList(this.objects)

        unions.forEach { union ->
            if(union.members.isEmpty()){
                throw SchemaException("The union type '${union.name}' has no possible types defined, requires at least one. Please refer to https://kgraphql.io/Reference/Type%20System/unions/")
            }
            union.members.forEach { member ->
                validateUnionMember(union, member, compiledObjects)
            }
        }

        return SchemaDefinition(compiledObjects, queries, scalars, mutations, subscriptions, enums, unions, directives, inputObjects)
    }

    private fun validateUnionMember(union: TypeDef.Union,
                                    member: KClass<*>,
                                    compiledObjects: ArrayList<TypeDef.Object<*>>) {
        if (scalars.any { it.kClass == member } || enums.any { it.kClass == member }) {
            throw SchemaException(
                "The member types of a Union type must all be Object base types; " +
                        "Scalar, Interface and Union types may not be member types of a Union"
            )
        }

        // KMP-safe check: verify member is not a Collection or Map type.
        // Uses qualifiedName instead of isKotlinSubclassOf reflection.
        val memberQn = member.qualifiedName
        if (memberQn != null && (memberQn.startsWith("kotlin.collections.") || memberQn.startsWith("java.util."))) {
            throw SchemaException("Collection/Map may not be member type of a Union '${union.name}'")
        }

        if (compiledObjects.none { it.kClass == member }) {
            compiledObjects.add(TypeDef.Object(member.defaultKQLTypeName(), member))
        }
    }

    fun addQuery(query : QueryDef<*>){
        if(query.checkEqualName(queries)){
            throw SchemaException("Cannot add query with duplicated name ${query.name}")
        }
        queries.add(query)
    }

    fun addMutation(mutation : MutationDef<*>){
        if(mutation.checkEqualName(mutations)){
            throw SchemaException("Cannot add mutation with duplicated name ${mutation.name}")
        }
        mutations.add(mutation)
    }

    fun addSubscription(subscription: SubscriptionDef<*>){
        if(subscription.checkEqualName(subscriptions)){
            throw SchemaException("Cannot add mutation with duplicated name ${subscription.name}")
        }
        subscriptions.add(subscription)
    }

    fun addScalar(scalar: TypeDef.Scalar<*>) = addType(scalar, scalars, "Scalar")

    fun addEnum(enum: TypeDef.Enumeration<*>) = addType(enum, enums, "Enumeration")

    fun addObject(objectType: TypeDef.Object<*>) = addType(objectType, objects, "Object")

    fun addUnion(union: TypeDef.Union) = addType(union, unions, "Union")

    fun addInputObject(input : TypeDef.Input<*>) = addType(input, inputObjects, "Input")

    fun <T : Definition>addType(type: T, target: ArrayList<T>, typeCategory: String){
        if(type.name.startsWith("__")){
            throw SchemaException("Type name starting with \"__\" are excluded for introspection system")
        }
        if(type.checkEqualName(objects, scalars, unions, enums)){
            throw SchemaException("Cannot add $typeCategory type with duplicated name ${type.name}")
        }
        target.add(type)
    }

    private fun Definition.checkEqualName(vararg collections: List<Definition>) : Boolean {
        return collections.fold(false, { acc, list -> acc || list.any { it.equalName(this) } })
    }

    private fun Definition.equalName(other: Definition): Boolean {
        return this.name.equals(other.name, true)
    }
}

private fun create__SchemaDefinition() = TypeDSL(emptyList(), __Schema::class).apply {
    // KSP-only: explicitly declare all __Schema properties with typeOf<>() return
    // types. No memberPropertiesList() reflection (iOS-safe).
    property(__Schema::types, typeOf<List<__Type>>())
    property(__Schema::queryType, typeOf<__Type>())
    property(__Schema::mutationType, typeOf<__Type?>())
    property(__Schema::subscriptionType, typeOf<__Type?>())
    property(__Schema::directives, typeOf<List<__Directive>>())
}.toKQLObject()

private fun create__TypeDefinition() = TypeDSL(emptyList(), __Type::class).apply {
    // KSP-only: explicitly declare all __Type properties with typeOf<>() return
    // types. No memberPropertiesList() reflection (iOS-safe).
    property(__Type::kind, typeOf<TypeKind>())
    property(__Type::name, typeOf<String?>())
    property(__Type::description, typeOf<String>())
    property(__Type::fields, typeOf<List<__Field>?>())
    property(__Type::interfaces, typeOf<List<__Type>?>())
    property(__Type::possibleTypes, typeOf<List<__Type>?>())
    property(__Type::enumValues, typeOf<List<__EnumValue>?>())
    property(__Type::inputFields, typeOf<List<__InputValue>?>())
    property(__Type::ofType, typeOf<__Type?>())
    // Transformations apply to the declared properties above (matched by name).
    transformation(__Type::fields, "includeDeprecated") { fields: List<__Field>?, includeDeprecated: Boolean? ->
        if (includeDeprecated == true) fields else fields?.filterNot { it.isDeprecated }
    }
    transformation(__Type::enumValues, "includeDeprecated") { enumValues: List<__EnumValue>?, includeDeprecated: Boolean? ->
        if (includeDeprecated == true) enumValues else enumValues?.filterNot { it.isDeprecated }
    }
}.toKQLObject()

private fun create__DirectiveDefinition() = TypeDSL(
    emptyList(),
    __Directive::class
).apply {
    // KSP-only: explicitly declare all __Directive properties (inherited from
    // __Described + declared in __Directive) with typeOf<>() return types.
    property(__Directive::name, typeOf<String>())
    property(__Directive::description, typeOf<String?>())
    property(__Directive::locations, typeOf<List<DirectiveLocation>>())
    property(__Directive::args, typeOf<List<__InputValue>>())
    // Deprecated extension properties (computed from locations).
    property<Boolean>("onField") {
        resolver { dir: __Directive ->
            dir.locations.contains(DirectiveLocation.FIELD)
        }
        deprecate("Use `locations`.")
    }
    property<Boolean>("onFragment") {
        resolver { dir: __Directive ->
            dir.locations.containsAny(
                DirectiveLocation.FRAGMENT_SPREAD,
                DirectiveLocation.FRAGMENT_DEFINITION,
                DirectiveLocation.INLINE_FRAGMENT
            )
        }
        deprecate("Use `locations`.")
    }
    property<Boolean>("onOperation") {
        resolver { dir: __Directive ->
            dir.locations.containsAny(
                DirectiveLocation.QUERY,
                DirectiveLocation.MUTATION,
                DirectiveLocation.SUBSCRIPTION
            )
        }
        deprecate("Use `locations`.")
    }
}.toKQLObject()

private fun create__FieldDefinition() = TypeDSL(emptyList(), __Field::class).apply {
    // KSP-only: explicitly declare all __Field properties (inherited from
    // __Described + Depreciable + declared in __Field) with typeOf<>() return types.
    property(__Field::name, typeOf<String>())
    property(__Field::description, typeOf<String?>())
    property(__Field::type, typeOf<__Type>())
    property(__Field::args, typeOf<List<__InputValue>>())
    property(__Field::isDeprecated, typeOf<Boolean>())
    property(__Field::deprecationReason, typeOf<String?>())
}.toKQLObject()

private fun create__InputValueDefinition() = TypeDSL(emptyList(), __InputValue::class).apply {
    // KSP-only: explicitly declare all __InputValue properties (inherited from
    // __Described + declared in __InputValue) with typeOf<>() return types.
    property(__InputValue::name, typeOf<String>())
    property(__InputValue::description, typeOf<String?>())
    property(__InputValue::type, typeOf<__Type>())
    property(__InputValue::defaultValue, typeOf<String?>())
}.toKQLObject()

private fun create__EnumValueDefinition() = TypeDSL(emptyList(), __EnumValue::class).apply {
    // KSP-only: explicitly declare all __EnumValue properties (inherited from
    // __Described + Depreciable) with typeOf<>() return types.
    property(__EnumValue::name, typeOf<String>())
    property(__EnumValue::description, typeOf<String?>())
    property(__EnumValue::isDeprecated, typeOf<Boolean>())
    property(__EnumValue::deprecationReason, typeOf<String?>())
}.toKQLObject()

private fun <T> List<T>.containsAny(vararg elements: T) = elements.filter { this.contains(it) }.any()

