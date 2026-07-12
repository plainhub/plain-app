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
import kotlin.reflect.KProperty1
import kotlin.reflect.typeOf


open class TypeDSL<T : Any>(
    private val supportedUnions: Collection<TypeDef.Union>,
    val kClass: KClass<T>
) : ItemDSL() {

    var name = kClass.defaultKQLTypeName()

    @PublishedApi
    internal val transformationProperties = mutableSetOf<Transformation<T, *>>()

    internal val extensionProperties = mutableSetOf<PropertyDef.Function<T, *>>()

    internal val unionProperties = mutableSetOf<PropertyDef.Union<T>>()

    internal val describedKotlinProperties = mutableMapOf<KProperty1<T, *>, PropertyDef.Kotlin<T, *>>()

    val dataloadedExtensionProperties = mutableSetOf<PropertyDef.DataLoadedFunction<T, *, *>>()

    // R is receiver (property value), 1 GraphQL arg
    inline fun <reified R, reified E> transformation(kProperty: KProperty1<T, R>, argName: String, noinline function: suspend (R, E) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(argName, function)))
    }

    // R is receiver, 2 GraphQL args
    inline fun <reified R, reified E, reified W> transformation(kProperty: KProperty1<T, R>, argName1: String, argName2: String, noinline function: suspend (R, E, W) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(argName1, argName2, function)))
    }

    // R is receiver, 3 GraphQL args
    inline fun <reified R, reified E, reified W, reified Q> transformation(kProperty: KProperty1<T, R>, argName1: String, argName2: String, argName3: String, noinline function: suspend (R, E, W, Q) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(argName1, argName2, argName3, function)))
    }

    // R is receiver, 4 GraphQL args
    inline fun <reified R, reified E, reified W, reified Q, reified A> transformation(kProperty: KProperty1<T, R>, argName1: String, argName2: String, argName3: String, argName4: String, noinline function: suspend (R, E, W, Q, A) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(argName1, argName2, argName3, argName4, function)))
    }

    // R is receiver, 5 GraphQL args
    inline fun <reified R, reified E, reified W, reified Q, reified A, reified S> transformation(kProperty: KProperty1<T, R>, argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, noinline function: suspend (R, E, W, Q, A, S) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, function)))
    }

    // R is receiver, 6 GraphQL args
    inline fun <reified R, reified E, reified W, reified Q, reified A, reified S, reified B> transformation(kProperty: KProperty1<T, R>, argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, noinline function: suspend (R, E, W, Q, A, S, B) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, argName6, function)))
    }

    // R is receiver, 7 GraphQL args
    inline fun <reified R, reified E, reified W, reified Q, reified A, reified S, reified B, reified U> transformation(kProperty: KProperty1<T, R>, argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, argName7: String, noinline function: suspend (R, E, W, Q, A, S, B, U) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, argName6, argName7, function)))
    }

    // R is receiver, 8 GraphQL args
    inline fun <reified R, reified E, reified W, reified Q, reified A, reified S, reified B, reified U, reified C> transformation(kProperty: KProperty1<T, R>, argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, argName7: String, argName8: String, noinline function: suspend (R, E, W, Q, A, S, B, U, C) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(argName1, argName2, argName3, argName4, argName5, argName6, argName7, argName8, function)))
    }

    @OptIn(ExperimentalStdlibApi::class)
    inline fun <KEY, reified TYPE> dataProperty(name: String, noinline block: DataLoaderPropertyDSL<T, KEY, TYPE>.() -> Unit) {
        dataloadedExtensionProperties.add(
            DataLoaderPropertyDSL(name, typeOf<TYPE>(), block).toKQLProperty()
        )
    }

    fun <R> property(kProperty: KProperty1<T, R>, block : KotlinPropertyDSL<T, R>.() -> Unit){
        val dsl = KotlinPropertyDSL(kProperty, block)
        describedKotlinProperties[kProperty] = dsl.toKQLProperty()
    }

    fun <R> property(name : String, block : PropertyDSL<T, R>.() -> Unit){
        val dsl = PropertyDSL(name, block)
        extensionProperties.add(dsl.toKQLProperty())
    }

    fun <R> KProperty1<T, R>.configure(block : KotlinPropertyDSL<T, R>.() -> Unit){
        property(this, block)
    }

    fun <R> KProperty1<T, R>.ignore(){
        describedKotlinProperties[this] = PropertyDef.Kotlin(kProperty = this, isIgnored = true)
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
            transformations = transformationProperties.associateBy { it.kProperty },
            description = description
        )
    }
}
