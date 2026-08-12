package com.ismartcoding.plain.lib.kgraphql.schema.dsl

import com.ismartcoding.plain.lib.kgraphql.defaultKQLTypeName
import com.ismartcoding.plain.lib.kgraphql.schema.Publisher
import com.ismartcoding.plain.lib.kgraphql.schema.Schema
import com.ismartcoding.plain.lib.kgraphql.schema.SchemaException
import com.ismartcoding.plain.lib.kgraphql.generated.GeneratedSchemaRegistry
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.operations.MutationDSL
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.operations.QueryDSL
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.operations.SubscriptionDSL
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.types.*
import com.ismartcoding.plain.lib.kgraphql.schema.model.EnumValueDef
import com.ismartcoding.plain.lib.kgraphql.schema.model.MutableSchemaDefinition
import com.ismartcoding.plain.lib.kgraphql.schema.model.TypeDef
import com.ismartcoding.plain.lib.kgraphql.schema.structure.SchemaCompilation
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.types.BooleanScalarDSL
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.types.DoubleScalarDSL
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.types.EnumDSL
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.types.InputTypeDSL
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.types.IntScalarDSL
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.types.LongScalarDSL
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.types.ScalarDSL
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.types.ShortScalarDSL
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.types.StringScalarDSL
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.types.TypeDSL
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.types.UnionTypeDSL
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.long
import kotlin.reflect.KClass

/**
 * SchemaBuilder exposes rich DSL to setup GraphQL schema
 */
class SchemaBuilder internal constructor() {

    private val model = MutableSchemaDefinition()

    var configuration = SchemaConfigurationDSL()

    fun build(): Schema {
        return runBlocking {
            SchemaCompilation(configuration.build(), model.toSchemaDefinition()).perform()
        }
    }

    fun configure(block: SchemaConfigurationDSL.() -> Unit){
        configuration.update(block)
    }

    //================================================================================
    // OPERATIONS
    //================================================================================

    fun query(name: String, init: QueryDSL.() -> Unit): Publisher {
        val query = QueryDSL(name)
            .apply(init)
            .toKQLQuery()
        model.addQuery(query)
        return query
    }

    fun mutation(name: String, init: MutationDSL.() -> Unit): Publisher {
        val mutation = MutationDSL(name)
            .apply(init)
            .toKQLMutation()

        model.addMutation(mutation)
        return mutation
    }

    fun subscription(name : String, init: SubscriptionDSL.() -> Unit){
        val subscription = SubscriptionDSL(name)
            .apply(init)
            .toKQLSubscription()

        model.addSubscription(subscription)
    }

    //================================================================================
    // SCALAR
    //================================================================================

    fun <T : Any> stringScalar(kClass: KClass<T>, block: ScalarDSL<T, String>.() -> Unit) {
        val scalar = StringScalarDSL(kClass).apply(block)
        configuration.appendMapper(scalar, kClass)
        model.addScalar(TypeDef.Scalar(scalar.name, kClass, scalar.createCoercion(), scalar.description))
    }

    inline fun <reified T : Any> stringScalar(noinline block: ScalarDSL<T, String>.() -> Unit) {
        stringScalar(T::class, block)
    }

    fun <T : Any> shortScalar(kClass: KClass<T>, block: ScalarDSL<T, Short>.() -> Unit) {
        val scalar = ShortScalarDSL(kClass).apply(block)
        configuration.appendMapper(scalar, kClass)
        model.addScalar(TypeDef.Scalar(scalar.name, kClass, scalar.createCoercion(), scalar.description))
    }

    inline fun <reified T : Any> shortScalar(noinline block: ScalarDSL<T, Short>.() -> Unit) {
        shortScalar(T::class, block)
    }

    fun <T : Any> intScalar(kClass: KClass<T>, block: ScalarDSL<T, Int>.() -> Unit) {
        val scalar = IntScalarDSL(kClass).apply(block)
        configuration.appendMapper(scalar, kClass)
        model.addScalar(TypeDef.Scalar(scalar.name, kClass, scalar.createCoercion(), scalar.description))
    }

    inline fun <reified T : Any> intScalar(noinline block: ScalarDSL<T, Int>.() -> Unit) {
        intScalar(T::class, block)
    }

    fun <T : Any> floatScalar(kClass: KClass<T>, block: ScalarDSL<T, Double>.() -> Unit) {
        val scalar = DoubleScalarDSL(kClass).apply(block)
        configuration.appendMapper(scalar, kClass)
        model.addScalar(TypeDef.Scalar(scalar.name, kClass, scalar.createCoercion(), scalar.description))
    }

    inline fun <reified T : Any> floatScalar(noinline block: ScalarDSL<T, Double>.() -> Unit) {
        floatScalar(T::class, block)
    }

    fun <T : Any> longScalar(kClass: KClass<T>, block: ScalarDSL<T, Long>.() -> Unit) {
        val scalar = LongScalarDSL(kClass).apply(block)
        configuration.appendMapper(scalar, kClass)
        model.addScalar(TypeDef.Scalar(scalar.name, kClass, scalar.createCoercion(), scalar.description))
    }

    inline fun <reified T : Any> longScalar(noinline block: ScalarDSL<T, Long>.() -> Unit) {
        longScalar(T::class, block)
    }

    fun <T : Any> booleanScalar(kClass: KClass<T>, block: ScalarDSL<T, Boolean>.() -> Unit) {
        val scalar = BooleanScalarDSL(kClass).apply(block)
        configuration.appendMapper(scalar, kClass)
        model.addScalar(TypeDef.Scalar(scalar.name, kClass, scalar.createCoercion(), scalar.description))
    }

    inline fun <reified T : Any> booleanScalar(noinline block: ScalarDSL<T, Boolean>.() -> Unit) {
        booleanScalar(T::class, block)
    }

    //================================================================================
    // TYPE
    //================================================================================

    fun <T : Any> type(kClass: KClass<T>, block: TypeDSL<T>.() -> Unit) {
        val type = TypeDSL(model.unionsMonitor, kClass).apply(block)
        // KSP merge: if a descriptor exists for this kClass, auto-declare any
        // properties the user did NOT explicitly add in [block]. This keeps
        // `type<User> {}` working without forcing users to re-list every field
        // already captured by @GraphQLType. Custom properties (transformations,
        // extensions, ignored) declared in [block] take precedence.
        GeneratedSchemaRegistry.types[kClass]?.let { desc ->
            @Suppress("UNCHECKED_CAST")
            val typedDesc = desc as com.ismartcoding.plain.lib.kgraphql.generated.TypeDescriptor<T>
            typedDesc.fields.forEach { f ->
                if (f.kProperty !in type.describedKotlinProperties) {
                    type.property(f.kProperty, f.returnType) {}
                }
            }
            // If the descriptor marks this as an interface, propagate the flag
            // and possibleTypes (no-op for regular @GraphQLType objects).
            if (typedDesc.isInterface) {
                type.interfaceType(typedDesc.possibleTypes)
            }
        }
        model.addObject(type.toKQLObject())
    }

    inline fun <reified T : Any> type(noinline block: TypeDSL<T>.() -> Unit) {
        type(T::class, block)
    }

    inline fun <reified T : Any> type() {
        type(T::class, {})
    }

    //================================================================================
    // ENUM
    //================================================================================

    fun <T : Enum<T>> enum(kClass: KClass<T>, enumValues: Array<T>, block: (EnumDSL<T>.() -> Unit)? = null) {
        val type = EnumDSL(kClass).apply {
            if (block != null) {
                block()
            }
        }

        val kqlEnumValues = enumValues.map { value ->
            type.valueDefinitions[value]?.let { valueDSL ->
                EnumValueDef(
                    value = value,
                    description = valueDSL.description,
                    isDeprecated = valueDSL.isDeprecated,
                    deprecationReason = valueDSL.deprecationReason
                )
            } ?: EnumValueDef(value)
        }

        model.addEnum(TypeDef.Enumeration(type.name, kClass, kqlEnumValues, type.description))

        // KMP-safe enum deserializer: register a JsonElement -> enum value mapper
        // so VariablesJson can convert JSON string variables to enum constants
        // WITHOUT using isKotlinEnum()/enumValueOfSafe() reflection (iOS-safe).
        val nameToValue = enumValues.associateBy { it.name }
        configuration.scalarDeserializers[kClass] = { element ->
            val name = (element as? JsonPrimitive)?.content
                ?: throw IllegalStateException("Expected JSON string for enum $kClass")
            nameToValue[name]
                ?: throw IllegalArgumentException("No enum constant ${kClass.qualifiedName}.$name")
        }
    }

    inline fun <reified T : Enum<T>> enum(noinline block: (EnumDSL<T>.() -> Unit)? = null) {
        val enumValues = enumValues<T>()
        if(enumValues.isEmpty()){
            throw SchemaException("Enum of type ${T::class} must have at least one value")
        } else {
            enum(T::class, enumValues<T>(), block)
        }
    }

    //================================================================================
    // UNION
    //================================================================================

    fun unionType(name: String, block: UnionTypeDSL.() -> Unit): TypeID {
        val union = UnionTypeDSL().apply(block)
        model.addUnion(TypeDef.Union(name, union.possibleTypes, union.description))
        return TypeID(name)
    }

    //================================================================================
    // INPUT
    //================================================================================

    fun <T : Any> inputType(kClass: KClass<T>, block: InputTypeDSL<T>.() -> Unit) {
        val input = InputTypeDSL(kClass).apply(block)
        // KSP merge: if an InputDescriptor exists for this kClass, auto-declare
        // any properties the user did NOT explicitly add in [block]. This keeps
        // `inputType<CreateItemInput>()` working without re-listing every field
        // already captured by @GraphQLInput.
        GeneratedSchemaRegistry.inputs[kClass]?.let { desc ->
            @Suppress("UNCHECKED_CAST")
            val typedDesc = desc as com.ismartcoding.plain.lib.kgraphql.generated.InputDescriptor<T>
            val declaredNames = input.declaredKotlinProperties.map { it.name }.toMutableSet()
            typedDesc.fields.forEach { f ->
                if (f.name !in declaredNames) {
                    input.property(f.kProperty, f.returnType)
                    declaredNames.add(f.name)
                }
            }
        }
        model.addInputObject(input.toKQLInput())
    }

    inline fun <reified T : Any> inputType(noinline block : InputTypeDSL<T>.() -> Unit = {}) {
        inputType(T::class, block)
    }
}

inline fun <T: Any, reified Raw: Any> SchemaConfigurationDSL.appendMapper(scalar: ScalarDSL<T, Raw>, kClass: KClass<T>) {
    scalarDeserializers[kClass] = { element ->
        val primitive = element as? JsonPrimitive
            ?: throw IllegalStateException("Expected JSON primitive for scalar $kClass")
        @Suppress("UNCHECKED_CAST")
        val raw: Raw = when (Raw::class) {
            String::class -> primitive.content as Raw
            Int::class -> primitive.int as Raw
            Long::class -> primitive.long as Raw
            Double::class -> primitive.double as Raw
            Boolean::class -> primitive.boolean as Raw
            else -> throw IllegalStateException("Unsupported raw type ${Raw::class} for scalar $kClass")
        }
        scalar.deserialize?.invoke(raw)
    }
}
