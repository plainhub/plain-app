package com.ismartcoding.plain.lib.kgraphql.schema.model

import com.ismartcoding.plain.lib.kgraphql.defaultKQLTypeName
import com.ismartcoding.plain.lib.kgraphql.generated.GeneratedSchemaRegistry
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
        // Introspection-reserved names — must NOT be derived from
        // KClass.simpleName (obfuscated in release builds).
        TypeDef.Enumeration(
            "__TypeKind",
            TypeKind::class,
            enumValues<TypeKind>().map { EnumValueDef(it) }
        ),
        TypeDef.Enumeration(
            "__DirectiveLocation",
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
            // Prefer the KSP-fixed name; simpleName is unsafe under obfuscation.
            val name = GeneratedSchemaRegistry.types[member]?.name ?: member.defaultKQLTypeName()
            compiledObjects.add(TypeDef.Object(name, member))
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

// Introspection types use spec-reserved names and accessor lambdas — no
// KProperty1 handles, no simpleName (both break under R8 obfuscation).
private fun create__SchemaDefinition() = TypeDSL(emptyList(), __Schema::class).apply {
    name = "__Schema"
    property("types", typeOf<List<__Type>>(), accessor = { it.types })
    property("queryType", typeOf<__Type>(), accessor = { it.queryType })
    property("mutationType", typeOf<__Type?>(), accessor = { it.mutationType })
    property("subscriptionType", typeOf<__Type?>(), accessor = { it.subscriptionType })
    property("directives", typeOf<List<__Directive>>(), accessor = { it.directives })
}.toKQLObject()

private fun create__TypeDefinition() = TypeDSL(emptyList(), __Type::class).apply {
    name = "__Type"
    property("kind", typeOf<TypeKind>(), accessor = { it.kind })
    property("name", typeOf<String?>(), accessor = { it.name })
    property("description", typeOf<String>(), accessor = { it.description })
    property("fields", typeOf<List<__Field>?>(), accessor = { it.fields })
    property("interfaces", typeOf<List<__Type>?>(), accessor = { it.interfaces })
    property("possibleTypes", typeOf<List<__Type>?>(), accessor = { it.possibleTypes })
    property("enumValues", typeOf<List<__EnumValue>?>(), accessor = { it.enumValues })
    property("inputFields", typeOf<List<__InputValue>?>(), accessor = { it.inputFields })
    property("ofType", typeOf<__Type?>(), accessor = { it.ofType })
    // Transformations apply to the declared properties above (matched by name).
    transformation("fields", "includeDeprecated") { fields: List<__Field>?, includeDeprecated: Boolean? ->
        if (includeDeprecated == true) fields else fields?.filterNot { it.isDeprecated }
    }
    transformation("enumValues", "includeDeprecated") { enumValues: List<__EnumValue>?, includeDeprecated: Boolean? ->
        if (includeDeprecated == true) enumValues else enumValues?.filterNot { it.isDeprecated }
    }
}.toKQLObject()

private fun create__DirectiveDefinition() = TypeDSL(
    emptyList(),
    __Directive::class
).apply {
    name = "__Directive"
    property("name", typeOf<String>(), accessor = { it.name })
    property("description", typeOf<String?>(), accessor = { it.description })
    property("locations", typeOf<List<DirectiveLocation>>(), accessor = { it.locations })
    property("args", typeOf<List<__InputValue>>(), accessor = { it.args })
}.toKQLObject()

private fun create__FieldDefinition() = TypeDSL(emptyList(), __Field::class).apply {
    name = "__Field"
    property("name", typeOf<String>(), accessor = { it.name })
    property("description", typeOf<String?>(), accessor = { it.description })
    property("type", typeOf<__Type>(), accessor = { it.type })
    property("args", typeOf<List<__InputValue>>(), accessor = { it.args })
    property("isDeprecated", typeOf<Boolean>(), accessor = { it.isDeprecated })
    property("deprecationReason", typeOf<String?>(), accessor = { it.deprecationReason })
}.toKQLObject()

private fun create__InputValueDefinition() = TypeDSL(emptyList(), __InputValue::class).apply {
    name = "__InputValue"
    property("name", typeOf<String>(), accessor = { it.name })
    property("description", typeOf<String?>(), accessor = { it.description })
    property("type", typeOf<__Type>(), accessor = { it.type })
    property("defaultValue", typeOf<String?>(), accessor = { it.defaultValue })
}.toKQLObject()

private fun create__EnumValueDefinition() = TypeDSL(emptyList(), __EnumValue::class).apply {
    name = "__EnumValue"
    property("name", typeOf<String>(), accessor = { it.name })
    property("description", typeOf<String?>(), accessor = { it.description })
    property("isDeprecated", typeOf<Boolean>(), accessor = { it.isDeprecated })
    property("deprecationReason", typeOf<String?>(), accessor = { it.deprecationReason })
}.toKQLObject()

private fun <T> List<T>.containsAny(vararg elements: T) = elements.filter { this.contains(it) }.any()

