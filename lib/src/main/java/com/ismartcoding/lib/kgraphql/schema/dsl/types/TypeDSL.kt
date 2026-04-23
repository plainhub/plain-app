package com.ismartcoding.lib.kgraphql.schema.dsl.types

import com.ismartcoding.lib.kgraphql.defaultKQLTypeName
import com.ismartcoding.lib.kgraphql.schema.SchemaException
import com.ismartcoding.lib.kgraphql.schema.dsl.*
import com.ismartcoding.lib.kgraphql.schema.model.FunctionWrapper
import com.ismartcoding.lib.kgraphql.schema.model.PropertyDef
import com.ismartcoding.lib.kgraphql.schema.model.Transformation
import com.ismartcoding.lib.kgraphql.schema.model.TypeDef
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.typeOf


open class TypeDSL<T : Any>(
    private val supportedUnions: Collection<TypeDef.Union>,
    val kClass: KClass<T>
) : ItemDSL() {

    var name = kClass.defaultKQLTypeName()

    internal val transformationProperties = mutableSetOf<Transformation<T, *>>()

    internal val extensionProperties = mutableSetOf<PropertyDef.Function<T, *>>()

    internal val unionProperties = mutableSetOf<PropertyDef.Union<T>>()

    internal val describedKotlinProperties = mutableMapOf<KProperty1<T, *>, PropertyDef.Kotlin<T, *>>()

    val dataloadedExtensionProperties = mutableSetOf<PropertyDef.DataLoadedFunction<T, *, *>>()

    fun <R, E> transformation(kProperty: KProperty1<T, R>, function: suspend (R, E) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(function, true)))
    }

    fun <R, E, W> transformation(kProperty: KProperty1<T, R>, function: suspend (R, E, W) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(function, true)))
    }

    fun <R, E, W, Q> transformation(kProperty: KProperty1<T, R>, function: suspend (R, E, W, Q) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(function, true)))
    }

    fun <R, E, W, Q, A> transformation(kProperty: KProperty1<T, R>, function: suspend (R, E, W, Q, A) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(function, true)))
    }

    fun <R, E, W, Q, A, S> transformation(kProperty: KProperty1<T, R>, function: suspend (R, E, W, Q, A, S) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(function, true)))
    }

    fun <R, E, W, Q, A, S, B> transformation(kProperty: KProperty1<T, R>, function: suspend (R, E, W, Q, A, S, B) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(function, true)))
    }

    fun <R, E, W, Q, A, S, B, U> transformation(kProperty: KProperty1<T, R>, function: suspend (R, E, W, Q, A, S, B, U) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(function, true)))
    }

    fun <R, E, W, Q, A, S, B, U, C> transformation(kProperty: KProperty1<T, R>, function: suspend (R, E, W, Q, A, S, B, U, C) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(function, true)))
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
