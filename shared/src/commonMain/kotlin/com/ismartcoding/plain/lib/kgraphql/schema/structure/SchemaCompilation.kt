@file:Suppress("LocalVariableName")

package com.ismartcoding.plain.lib.kgraphql.schema.structure

import com.ismartcoding.plain.lib.kgraphql.Context
import com.ismartcoding.plain.lib.kgraphql.configuration.SchemaConfiguration
import com.ismartcoding.plain.lib.kgraphql.defaultKQLTypeName
import com.ismartcoding.plain.lib.kgraphql.getIterableElementType
import com.ismartcoding.plain.lib.kgraphql.isIterable
import com.ismartcoding.plain.lib.kgraphql.schema.DefaultSchema
import com.ismartcoding.plain.lib.kgraphql.schema.SchemaException
import com.ismartcoding.plain.lib.kgraphql.schema.directive.Directive
import com.ismartcoding.plain.lib.kgraphql.schema.execution.Execution
import com.ismartcoding.plain.lib.kgraphql.schema.introspection.NotIntrospected
import com.ismartcoding.plain.lib.kgraphql.schema.introspection.SchemaProxy
import com.ismartcoding.plain.lib.kgraphql.schema.introspection.TypeKind
import com.ismartcoding.plain.lib.kgraphql.schema.introspection.__Schema
import com.ismartcoding.plain.lib.kgraphql.schema.model.BaseOperationDef
import com.ismartcoding.plain.lib.kgraphql.schema.model.FunctionWrapper
import com.ismartcoding.plain.lib.kgraphql.schema.model.InputValueDef
import com.ismartcoding.plain.lib.kgraphql.schema.model.PropertyDef
import com.ismartcoding.plain.lib.kgraphql.schema.model.QueryDef
import com.ismartcoding.plain.lib.kgraphql.schema.model.SchemaDefinition
import com.ismartcoding.plain.lib.kgraphql.schema.model.Transformation
import com.ismartcoding.plain.lib.kgraphql.schema.model.TypeDef
import com.ismartcoding.plain.lib.kgraphql.hasNotIntrospectedAnnotation
import com.ismartcoding.plain.lib.kgraphql.isKotlinFinal
import com.ismartcoding.plain.lib.kgraphql.isKotlinSealed
import com.ismartcoding.plain.lib.kgraphql.isKotlinSubclassOf
import com.ismartcoding.plain.lib.kgraphql.isKotlinSuperclassOf
import com.ismartcoding.plain.lib.kgraphql.isPublicVisibility
import com.ismartcoding.plain.lib.kgraphql.kClass
import com.ismartcoding.plain.lib.kgraphql.memberPropertiesList
import com.ismartcoding.plain.lib.kgraphql.sealedSubclassesList
import kotlin.collections.get
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType

@Suppress("UNCHECKED_CAST")
class SchemaCompilation(
    val configuration : SchemaConfiguration,
    val definition : SchemaDefinition
){

    private val queryTypeProxies = mutableMapOf<KClass<*>, TypeProxy>()

    private val inputTypeProxies = mutableMapOf<KClass<*>, TypeProxy>()

    private val unions = mutableListOf<Type.Union>()

    private val enums = definition.enums.associate { enum -> enum.kClass to enum.toEnumType() }

    private val scalars = definition.scalars.associate { scalar -> scalar.kClass to scalar.toScalarType() }

    private val schemaProxy = SchemaProxy(configuration)

    private val contextType = Type._Context()

    private val executionType = Type._ExecutionNode()

    private enum class TypeCategory {
        INPUT, QUERY
    }

    suspend fun perform(): DefaultSchema {
        definition.unions.forEach { handleUnionType(it) }
        definition.objects.forEach { handleObjectType(it.kClass) }
        definition.inputObjects.forEach { handleInputType(it.kClass) }
        val queryType = handleQueries()
        val mutationType = handleMutations()
        val subscriptionType = handleSubscriptions()

        queryTypeProxies.forEach { (kClass, typeProxy) ->
            introspectPossibleTypes(kClass, typeProxy)
            introspectInterfaces(kClass, typeProxy)
        }

        val model =  SchemaModel (
            query = queryType,
            mutation = if (mutationType.fields!!.isEmpty()) null else mutationType,
            subscription = if (subscriptionType.fields!!.isEmpty()) null else subscriptionType,

            enums = enums,
            scalars = scalars,
            unions = unions,

            queryTypes = queryTypeProxies + enums + scalars,
            inputTypes = inputTypeProxies + enums + scalars,
            allTypes = queryTypeProxies.values
                    + inputTypeProxies.values
                    + enums.values
                    + scalars.values
                    + unions.distinctBy(Type.Union::name),
            directives = definition.directives.map { handlePartialDirective(it) }
        )
        val schema = DefaultSchema(configuration, model)
        schemaProxy.proxiedSchema = schema
        return schema
    }

    private fun introspectPossibleTypes(kClass: KClass<*>, typeProxy: TypeProxy) {
        val proxied = typeProxy.proxied
        if (proxied is Type.Interface<*>) {
            val possibleTypes = queryTypeProxies.filter { (otherKClass, otherTypeProxy) ->
                otherTypeProxy.kind == TypeKind.OBJECT && otherKClass != kClass && otherKClass.isKotlinSubclassOf(kClass)
            }.values.toList()

            typeProxy.proxied = proxied.withPossibleTypes(possibleTypes)
        }
    }

    private fun introspectInterfaces(kClass: KClass<*>, typeProxy: TypeProxy) {
        val proxied = typeProxy.proxied
        if(proxied is Type.Object<*>){
            val interfaces = queryTypeProxies.filter { (otherKClass, otherTypeProxy) ->
                otherTypeProxy.kind == TypeKind.INTERFACE && otherKClass != kClass && kClass.isKotlinSubclassOf(otherKClass)
            }.values.toList()

            typeProxy.proxied = proxied.withInterfaces(interfaces)
        }
    }

    private suspend fun handlePartialDirective(directive: Directive.Partial) : Directive {
        val inputValues = handleInputValues(directive.name, directive.execution, emptyList())
        return directive.toDirective(inputValues)
    }

    private suspend fun handleQueries() : Type {
        return Type.OperationObject(
            name = "Query",
            description = "Query object",
            fields = definition.queries.map { handleOperation(it) } + introspectionSchemaQuery() + introspectionTypeQuery()
        )
    }

    private suspend fun handleMutations() : Type {
        return Type.OperationObject("Mutation", "Mutation object", definition.mutations.map { handleOperation(it) })
    }

    private suspend fun handleSubscriptions() : Type {
        return Type.OperationObject("Subscription", "Subscription object", definition.subscriptions.map { handleOperation(it) })
    }

    @Suppress("USELESS_CAST") // We are casting as __Schema so we don't get proxied types. https://github.com/aPureBase/KGraphQL/issues/45
    private suspend fun introspectionSchemaQuery() = handleOperation(
        QueryDef("__schema", FunctionWrapper.on<__Schema> { schemaProxy as __Schema })
    )

    private suspend fun introspectionTypeQuery() = handleOperation(
        QueryDef("__type", FunctionWrapper.on("name") { name: String ->
            schemaProxy.findTypeByName(name)
        })
    )

    private suspend fun handleOperation(operation : BaseOperationDef<*, *>) : Field {
        val returnType = handlePossiblyWrappedType(operation.returnType, TypeCategory.QUERY)
        val inputValues = handleInputValues(operation.name, operation, operation.inputValues)
        return Field.Function(operation, returnType, inputValues)
    }

    private suspend fun handleUnionProperty(unionProperty: PropertyDef.Union<*>) : Field {
        val inputValues = handleInputValues(unionProperty.name, unionProperty, unionProperty.inputValues)
        val type = handleUnionType(unionProperty.union)

        return Field.Union(unionProperty, unionProperty.nullable, type, inputValues)
    }

    private suspend fun handlePossiblyWrappedType(kType : KType, typeCategory: TypeCategory) : Type = try {
        when {
            kType.isIterable() -> handleCollectionType(kType, typeCategory)
            kType.kClass() == Context::class && typeCategory == TypeCategory.INPUT -> contextType
            kType.kClass() == Execution.Node::class && typeCategory == TypeCategory.INPUT -> executionType
            kType.kClass() == Context::class && typeCategory == TypeCategory.QUERY -> throw SchemaException("Context type cannot be part of schema")
            kType.arguments.isNotEmpty() -> configuration.genericTypeResolver.resolveMonad(kType)
                .let { handlePossiblyWrappedType(it, typeCategory) }
            kType.kClass().isKotlinSealed() -> TypeDef.Union(
                name = kType.kClass().simpleName!!,
                members = kType.kClass().sealedSubclassesList().toSet(),
                description = null
            ).let { handleUnionType(it) }
            else -> handleSimpleType(kType, typeCategory)
        }
    } catch (e: Throwable) {
        if ("KotlinReflectionInternalError" in e.toString()) {
            throw SchemaException("If you construct a query/mutation generically, you must specify the return type T explicitly with resolver{ ... }.returns<T>()")
        } else {
            throw e
        }
    }

    private suspend fun handleCollectionType(kType: KType, typeCategory: TypeCategory): Type {
        val type = when {
            kType.getIterableElementType() != null -> kType.getIterableElementType()
            kType.arguments.size == 1 -> kType.arguments.first().type
            else -> null
        } ?: throw throw SchemaException("Cannot handle collection without element type")

        val nullableListType = Type.AList(handleSimpleType(type, typeCategory))
        return applyNullability(kType, nullableListType)
    }

    private suspend fun handleSimpleType(kType: KType, typeCategory: TypeCategory): Type {
        val simpleType = handleRawType(kType.kClass(), typeCategory)
        return applyNullability(kType, simpleType)
    }

    private fun applyNullability(kType: KType, simpleType: Type): Type {
        if (!kType.isMarkedNullable) {
            return Type.NonNull(simpleType)
        } else {
            return simpleType
        }
    }

    private suspend fun handleRawType(kClass: KClass<*>, typeCategory: TypeCategory) : Type {
        when (val type = unions.find { it.name == kClass.simpleName }) {
            null -> Unit
            else -> return type
        }

        if(kClass == Context::class) throw SchemaException("Context type cannot be part of schema")

        val cachedInstances = when(typeCategory) {
            TypeCategory.QUERY -> queryTypeProxies
            TypeCategory.INPUT -> inputTypeProxies
        }


        return cachedInstances[kClass]
            ?: enums[kClass]
            ?: scalars[kClass]
            ?: when(typeCategory) {
                TypeCategory.QUERY -> handleObjectType(kClass)
                TypeCategory.INPUT -> handleInputType(kClass)
            }
    }

    private suspend fun <T, K, R> handleDataloadOperation(
        operation: PropertyDef.DataLoadedFunction<T, K, R>
    ): Field {
        val returnType = handlePossiblyWrappedType(operation.returnType, TypeCategory.QUERY)
        val inputValues = handleInputValues(operation.name, operation.prepare, operation.inputValues)

        return Field.DataLoader(
            kql = operation,
            loader = operation.loader,
            returnType = returnType,
            arguments = inputValues
        )
    }

    private suspend fun handleObjectType(kClass: KClass<*>) : Type {
        assertValidObjectType(kClass)
        val objectDefs = definition.objects.filter { it.kClass.isKotlinSuperclassOf(kClass) }
        val objectDef = objectDefs.find { it.kClass == kClass } ?: TypeDef.Object(kClass.defaultKQLTypeName(), kClass)

        //treat introspection types as objects -> adhere to reference implementation behaviour
        val kind = if(kClass.isKotlinFinal() || objectDef.name.startsWith("__")) TypeKind.OBJECT else TypeKind.INTERFACE

        val objectType = if(kind == TypeKind.OBJECT) Type.Object(objectDef) else Type.Interface(objectDef)
        val typeProxy = TypeProxy(objectType)
        queryTypeProxies[kClass] = typeProxy

        val allKotlinProperties = objectDefs.fold(emptyMap<String, PropertyDef.Kotlin<*, *>>()) { acc, def ->
            acc + def.kotlinProperties.mapKeys { (property) -> property.name }
        }
        val allTransformations= objectDefs.fold(emptyMap<String, Transformation<*, *>>()) { acc, def ->
            acc + def.transformations.mapKeys { (property) -> property.name }
        }

        val kotlinFields = kClass.memberPropertiesList()
                .filter { field -> field.isPublicVisibility() }
                .filterNot { field ->  objectDefs.any { it.isIgnored(field.name) } }
                .map { property -> handleKotlinProperty (
                        kProperty = property,
                        kqlProperty = allKotlinProperties[property.name],
                        transformation = allTransformations[property.name]
                ) }

        val extensionFields = objectDefs
            .flatMap(TypeDef.Object<*>::extensionProperties)
            .map { property -> handleOperation(property) }

        val dataloadExtensionFields = objectDefs
            .flatMap(TypeDef.Object<*>::dataloadExtensionProperties)
            .map { property -> handleDataloadOperation(property) }

        val unionFields = objectDefs
            .flatMap(TypeDef.Object<*>::unionProperties)
            .map { property -> handleUnionProperty(property) }


        val typenameResolver: suspend (Any) -> String? = { value: Any ->
            schemaProxy.typeByKClass(value::class)?.name ?: typeProxy.name
        }

        val __typenameField = handleOperation (
                PropertyDef.Function<Nothing, String?> ("__typename", FunctionWrapper.on(typenameResolver))
        )

        val declaredFields = kotlinFields + extensionFields + unionFields + dataloadExtensionFields

        if(declaredFields.isEmpty()){
            throw SchemaException("An Object type must define one or more fields. Found none on type ${objectDef.name}")
        }

        declaredFields.find { it.name.startsWith("__") }?.let { field ->
            throw SchemaException("Illegal name '${field.name}'. Names starting with '__' are reserved for introspection system")
        }

        val allFields = declaredFields + __typenameField
        typeProxy.proxied = if(kind == TypeKind.OBJECT) Type.Object(objectDef, allFields) else Type.Interface(objectDef, allFields)
        return typeProxy
    }

    private suspend fun handleInputType(kClass: KClass<*>) : Type {
        assertValidObjectType(kClass)

        val inputObjectDef = definition.inputObjects.find { it.kClass == kClass } ?: TypeDef.Input(kClass.defaultKQLTypeName(), kClass)
        val objectType = Type.Input(inputObjectDef)
        val typeProxy = TypeProxy(objectType)
        inputTypeProxies[kClass] = typeProxy

        val fields = if (!kClass.hasNotIntrospectedAnnotation()) {
            kClass.memberPropertiesList().map { property -> handleKotlinInputProperty(property) }
        } else listOf()

        typeProxy.proxied = Type.Input(inputObjectDef, fields)
        return typeProxy
    }

    private suspend fun handleInputValues(operationName : String, operation: FunctionWrapper<*>, inputValues: List<InputValueDef<*>>) : List<InputValue<*>> {
        val invalidInputValues = inputValues
                .map { it.name }
                .filterNot { it in operation.argumentsDescriptor.keys }

        if(invalidInputValues.isNotEmpty()){
            throw SchemaException("Invalid input values on $operationName: $invalidInputValues")
        }

        return operation.argumentsDescriptor.map { (name, kType) ->
            val inputValue = inputValues.find { it.name == name }
            val kqlInput = inputValue ?: InputValueDef(kType.kClass(), name)
            val inputType = handlePossiblyWrappedType(inputValue?.kType ?: kType, TypeCategory.INPUT)
            InputValue(kqlInput, inputType)
        }
    }

    private suspend fun handleUnionType(union : TypeDef.Union) : Type.Union {
        val possibleTypes = union.members.map {
            handleRawType(it, TypeCategory.QUERY)
        }

        val invalidPossibleTypes = possibleTypes.filterNot { it.kind == TypeKind.OBJECT }
        if(invalidPossibleTypes.isNotEmpty()){
            throw SchemaException("Invalid union type members")
        }

        val __typenameField = handleOperation (
            PropertyDef.Function<Nothing, String?> ("__typename", FunctionWrapper.on<String?, Any> { value: Any ->
                schemaProxy.typeByKClass(value::class)?.name
            })
        )

        val unionType = Type.Union(union, __typenameField, possibleTypes)
        unions.add(unionType)
        return unionType
    }

    private suspend fun handleKotlinInputProperty(kProperty: KProperty1<*, *>) : InputValue<*> {
        val type = handlePossiblyWrappedType(kProperty.returnType, TypeCategory.INPUT)
        return InputValue(InputValueDef(kProperty.returnType.kClass(), kProperty.name), type)
    }

    private suspend fun <T : Any, R> handleKotlinProperty (
        kProperty: KProperty1<T, R>,
        kqlProperty: PropertyDef.Kotlin<*, *>?,
        transformation: Transformation<*, *>?
    ) : Field.Kotlin<*, *> {
        val returnType = handlePossiblyWrappedType(kProperty.returnType, TypeCategory.QUERY)
        val inputValues = if(transformation != null){
            handleInputValues("$kProperty transformation", transformation.transformation, emptyList())
        } else {
            emptyList()
        }

        val actualKqlProperty = kqlProperty ?: PropertyDef.Kotlin(kProperty)

        return Field.Kotlin (
                kql = actualKqlProperty as PropertyDef.Kotlin<T, R>,
                returnType = returnType,
                arguments = inputValues,
                transformation = transformation as Transformation<T, R>?
        )
    }
}
