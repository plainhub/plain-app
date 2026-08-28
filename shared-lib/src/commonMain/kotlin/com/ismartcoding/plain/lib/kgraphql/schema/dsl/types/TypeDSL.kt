package com.ismartcoding.plain.lib.kgraphql.schema.dsl.types

import com.ismartcoding.plain.lib.kgraphql.defaultKQLTypeName
import com.ismartcoding.plain.lib.kgraphql.schema.SchemaException
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.*
import com.ismartcoding.plain.lib.kgraphql.schema.model.FunctionWrapper
import com.ismartcoding.plain.lib.kgraphql.schema.model.PropertyDef
import com.ismartcoding.plain.lib.kgraphql.schema.model.Transformation
import com.ismartcoding.plain.lib.kgraphql.schema.model.TypeDef
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.DataLoaderPropertyDSL
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.ItemDSL
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.KotlinPropertyDSL
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.PropertyDSL
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.UnionPropertyDSL
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf


open class TypeDSL<T : Any>(
    private val supportedUnions: Collection<TypeDef.Union>,
    val kClass: KClass<T>
) : ItemDSL() {

    var name = kClass.defaultKQLTypeName()
        set(value) {
            field = value
            nameCustomized = true
        }

    // True when the user set [name] explicitly in the DSL block; lets the KSP
    // merge override the (obfuscation-unsafe) simpleName default.
    internal var nameCustomized = false

    // KSP bridge: set to true for @GraphQLInterface types, bypasses isKotlinFinal()
    var isInterface: Boolean = false
        internal set

    // KSP bridge: explicit possible types for interfaces, bypasses isKotlinSubclassOf()
    var possibleTypes: List<KClass<*>> = emptyList()
        internal set

    /**
     * Marks this type as a GraphQL Interface with the given implementing types.
     * Used by registerGeneratedSchema() — replaces runtime isKotlinSubclassOf.
     */
    fun interfaceType(possible: List<KClass<*>> = emptyList()) {
        isInterface = true
        possibleTypes = possible
    }

    @PublishedApi
    internal val transformationProperties = mutableSetOf<Transformation<T, *>>()

    internal val extensionProperties = mutableSetOf<PropertyDef.Function<T, *>>()

    internal val unionProperties = mutableSetOf<PropertyDef.Union<T>>()

    internal val describedKotlinProperties = mutableMapOf<String, PropertyDef.Kotlin<T, *>>()

    val dataloadedExtensionProperties = mutableSetOf<PropertyDef.DataLoadedFunction<T, *, *>>()

    // R is receiver (property value), 1 GraphQL arg
    inline fun <reified R, reified E> transformation(name: String, argName: String, noinline function: suspend (R, E) -> R) {
        transformationProperties.add(Transformation(name, FunctionWrapper.on(argName, function)))
    }

    // R is receiver, 2 GraphQL args
    inline fun <reified R, reified E, reified W> transformation(name: String, argName1: String, argName2: String, noinline function: suspend (R, E, W) -> R) {
        transformationProperties.add(Transformation(name, FunctionWrapper.on(argName1, argName2, function)))
    }

    // R is receiver, 3 GraphQL args
    inline fun <reified R, reified E, reified W, reified Q> transformation(name: String, argName1: String, argName2: String, argName3: String, noinline function: suspend (R, E, W, Q) -> R) {
        transformationProperties.add(Transformation(name, FunctionWrapper.on(argName1, argName2, argName3, function)))
    }

    // R is receiver, 4 GraphQL args
    inline fun <reified R, reified E, reified W, reified Q, reified A> transformation(name: String, argName1: String, argName2: String, argName3: String, argName4: String, noinline function: suspend (R, E, W, Q, A) -> R) {
        transformationProperties.add(Transformation(name, FunctionWrapper.on(argName1, argName2, argName3, argName4, function)))
    }

    // R is receiver, 5 GraphQL args
    inline fun <reified R, reified E, reified W, reified Q, reified A, reified S> transformation(name: String, argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, noinline function: suspend (R, E, W, Q, A, S) -> R) {
        transformationProperties.add(Transformation(name, FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, function)))
    }

    // R is receiver, 6 GraphQL args
    inline fun <reified R, reified E, reified W, reified Q, reified A, reified S, reified B> transformation(name: String, argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, noinline function: suspend (R, E, W, Q, A, S, B) -> R) {
        transformationProperties.add(Transformation(name, FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, argName6, function)))
    }

    // R is receiver, 7 GraphQL args
    inline fun <reified R, reified E, reified W, reified Q, reified A, reified S, reified B, reified U> transformation(name: String, argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, argName7: String, noinline function: suspend (R, E, W, Q, A, S, B, U) -> R) {
        transformationProperties.add(Transformation(name, FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, argName6, argName7, function)))
    }

    // R is receiver, 8 GraphQL args
    inline fun <reified R, reified E, reified W, reified Q, reified A, reified S, reified B, reified U, reified C> transformation(name: String, argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, argName7: String, argName8: String, noinline function: suspend (R, E, W, Q, A, S, B, U, C) -> R) {
        transformationProperties.add(Transformation(name, FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, argName6, argName7, argName8, function)))
    }

    @OptIn(ExperimentalStdlibApi::class)
    inline fun <KEY, reified TYPE> dataProperty(name: String, noinline block: DataLoaderPropertyDSL<T, KEY, TYPE>.() -> Unit) {
        dataloadedExtensionProperties.add(
            DataLoaderPropertyDSL(name, typeOf<TYPE>(), block).toKQLProperty()
        )
    }

    fun <R> property(name : String, block : PropertyDSL<T, R>.() -> Unit){
        val dsl = PropertyDSL(name, block)
        extensionProperties.add(dsl.toKQLProperty())
    }

    /**
     * Registers a Kotlin property by name with a compile-time accessor lambda.
     * The [returnType] is generated at compile time via `typeOf<R>()`.
     */
    fun <R> property(name: String, returnType: KType, accessor: (T) -> R, block : KotlinPropertyDSL<T, R>.() -> Unit = {}){
        val dsl = KotlinPropertyDSL(name, accessor, returnType, block)
        describedKotlinProperties[name] = dsl.toKQLProperty()
    }

    fun ignore(name: String) {
        @Suppress("UNCHECKED_CAST")
        describedKotlinProperties[name] = PropertyDef.Kotlin(
            name = name,
            accessor = { _: T -> throw IllegalStateException("Property '$name' is ignored") },
            isIgnored = true
        )
    }

    fun unionProperty(name : String, block : UnionPropertyDSL<T>.() -> Unit){
        val property = UnionPropertyDSL(name, block)
        val union = supportedUnions.find { property.returnType.typeID.equals(it.name, true) }
            ?: throw SchemaException("Union Type: ${property.returnType.typeID} does not exist")

        unionProperties.add(property.toKQLProperty(union))
    }


    internal fun toKQLObject() : TypeDef.Object<T> {
        return TypeDef.Object(
            name = name,
            kClass = kClass,
            kotlinProperties = describedKotlinProperties.toMap(),
            extensionProperties = extensionProperties.toList(),
            dataloadExtensionProperties = dataloadedExtensionProperties.toList(),
            unionProperties = unionProperties.toList(),
            transformations = transformationProperties.associateBy { it.name },
            description = description,
            isInterface = isInterface,
            possibleTypes = possibleTypes
        )
    }
}
