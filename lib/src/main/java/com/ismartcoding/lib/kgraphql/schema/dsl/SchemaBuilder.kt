package com.ismartcoding.lib.kgraphql.schema.dsl

import com.ismartcoding.lib.kgraphql.schema.Publisher
import com.ismartcoding.lib.kgraphql.schema.Schema
import com.ismartcoding.lib.kgraphql.schema.SchemaException
import com.ismartcoding.lib.kgraphql.schema.dsl.operations.MutationDSL
import com.ismartcoding.lib.kgraphql.schema.dsl.operations.QueryDSL
import com.ismartcoding.lib.kgraphql.schema.dsl.operations.SubscriptionDSL
import com.ismartcoding.lib.kgraphql.schema.dsl.types.*
import com.ismartcoding.lib.kgraphql.schema.model.EnumValueDef
import com.ismartcoding.lib.kgraphql.schema.model.MutableSchemaDefinition
import com.ismartcoding.lib.kgraphql.schema.model.TypeDef
import com.ismartcoding.lib.kgraphql.schema.structure.SchemaCompilation
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
                EnumValueDef (
                    value = value,
                    description = valueDSL.description,
                    isDeprecated = valueDSL.isDeprecated,
                    deprecationReason = valueDSL.deprecationReason
                )
            } ?: EnumValueDef(value)
        }

        model.addEnum(TypeDef.Enumeration(type.name, kClass, kqlEnumValues, type.description))
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

    inline fun <reified T: Any> unionType(noinline block: UnionTypeDSL.() -> Unit = {}): TypeID {
        if (!T::class.isSealed) throw SchemaException("Can't generate a union type out of a non sealed class. '${T::class.simpleName}'")

        return unionType(T::class.simpleName!!) {
            block()
            T::class.sealedSubclasses.forEach {
                type(it, subTypeBlock) // <-- Adds to schema definition
                type(it) // <-- Adds to possible union type
            }
        }
    }

    //================================================================================
    // INPUT
    //================================================================================

    fun <T : Any> inputType(kClass: KClass<T>, block: InputTypeDSL<T>.() -> Unit) {
        val input = InputTypeDSL(kClass).apply(block)
        model.addInputObject(TypeDef.Input(input.name, kClass, input.description))
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
