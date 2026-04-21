package com.ismartcoding.lib.kgraphql.schema.execution

import com.ismartcoding.lib.kgraphql.deferredJson.DeferredJsonMap
import com.ismartcoding.lib.kgraphql.deferredJson.deferredJsonBuilder
import com.ismartcoding.lib.kgraphql.Context
import com.ismartcoding.lib.kgraphql.ExecutionException
import com.ismartcoding.lib.kgraphql.GraphQLError
import com.ismartcoding.lib.kgraphql.request.Variables
import com.ismartcoding.lib.kgraphql.request.VariablesJson
import com.ismartcoding.lib.kgraphql.schema.DefaultSchema
import com.ismartcoding.lib.kgraphql.schema.introspection.TypeKind
import com.ismartcoding.lib.kgraphql.schema.model.FunctionWrapper
import com.ismartcoding.lib.kgraphql.schema.model.TypeDef
import com.ismartcoding.lib.kgraphql.schema.model.ast.ArgumentNodes
import com.ismartcoding.lib.kgraphql.schema.scalar.serializeScalar
import com.ismartcoding.lib.kgraphql.schema.structure.Field
import com.ismartcoding.lib.kgraphql.schema.structure.InputValue
import com.ismartcoding.lib.kgraphql.schema.structure.Type
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import com.ismartcoding.lib.kdataloader.DataLoader
import kotlin.reflect.KProperty1


class DataLoaderPreparedRequestExecutor(val schema: DefaultSchema) : RequestExecutor {

    private val argumentsHandler = ArgumentsHandler(schema)

    inner class ExecutionContext(
        val variables: Variables,
        val requestContext: Context,
        val loaders: Map<Field.DataLoader<*, *, *>, DataLoader<Any?, *>>
    )

    private suspend fun ExecutionPlan.constructLoaders(): Map<Field.DataLoader<*, *, *>, DataLoader<Any?, *>> = coroutineScope {
        val loaders = mutableMapOf<Field.DataLoader<*, *, *>, DataLoader<Any?, *>>()

        suspend fun Collection<Execution>.look() {
            forEach { ex ->
                when (ex) {
                    is Execution.Fragment -> ex.elements.look()
                    is Execution.Node -> {
                        ex.children.look()
                        if (ex.field is Field.DataLoader<*, *, *>) {
                            loaders[ex.field] = ex.field.loader.constructNew(coroutineContext.job) as DataLoader<Any?, *>
                        }
                    }
                }
            }
        }
        operations.look()
        loaders
    }

    private suspend fun <T> DeferredJsonMap.writeOperation(
        ctx: ExecutionContext,
        node: Execution.Node,
        operation: FunctionWrapper<T>
    )  {
        node.field.checkAccess(null, ctx.requestContext)
        val result: T? = operation.invoke(
            funName = node.field.name,
            receiver = null,
            inputValues = node.field.arguments,
            args = node.arguments,
            executionNode = node,
            ctx = ctx
        )


        applyKeyToElement(ctx, result, node, node.field.returnType, 1)
    }

    private fun Any?.toPrimitive(node: Execution.Node, returnType: Type): JsonElement = when {
        this == null -> createNullNode(node, returnType.unwrapList())
        this is Collection<*> || this is Array<*> -> when (this) {
            is Array<*> -> this.toList()
            else -> this as Collection<*>
        }.map { it.toPrimitive(node, returnType.unwrapList()) }.let(::JsonArray)
        this is String -> JsonPrimitive(this)
        this is Int -> JsonPrimitive(this)
        this is Float -> JsonPrimitive(this)
        this is Double -> JsonPrimitive(this)
        this is Boolean -> JsonPrimitive(this)
        this is Long -> JsonPrimitive(this)
        returnType.unwrapped() is Type.Enum<*> -> JsonPrimitive(toString())
        else -> throw TODO("Whaa? -> $this")
    }

    private suspend fun <T> DeferredJsonMap.applyKeyToElement(
        ctx: ExecutionContext,
        value: T?,
        node: Execution.Node,
        returnType: Type,
        parentCount: Long
    ) {
        return when {
            value == null -> node.aliasOrKey toValue createNullNode(node, returnType)
            value is Collection<*> || value is Array<*> -> {
                if (returnType.isList()) {
                    val values = when (value) {
                        is Array<*> -> value.toList()
                        else -> value as Collection<*>
                    }

                    if (node.children.isEmpty()) {
                        node.aliasOrKey toDeferredArray {
                            values.map { addValue(it.toPrimitive(node, returnType)) }
                        }
                    } else {
                        node.aliasOrKey toDeferredArray {
                            values.map { v ->
                                when {
                                    v == null -> addValue(createNullNode(node, returnType.unwrapList()))
                                    node.children.isNotEmpty() -> addDeferredObj {
                                        this@addDeferredObj.applyObjectProperties(
                                            ctx = ctx,
                                            value = v,
                                            node = node,
                                            type = returnType.unwrapList(),
                                            parentCount = values.size.toLong()
                                        )
                                    }
                                    else -> throw TODO("Unknown error!")
                                }
                            }
                        }
                    }
                } else {
                    throw ExecutionException("Invalid collection value for non collection property", node)
                }
            }
            value is String -> node.aliasOrKey toValue JsonPrimitive(value)
            value is Int -> node.aliasOrKey toValue JsonPrimitive(value)
            value is Float -> node.aliasOrKey toValue JsonPrimitive(value)
            value is Double -> node.aliasOrKey toValue JsonPrimitive(value)
            value is Boolean -> node.aliasOrKey toValue JsonPrimitive(value)
            value is Long -> node.aliasOrKey toValue JsonPrimitive(value)
            value is Deferred<*> -> {
                deferredLaunch {
                    applyKeyToElement(ctx, value.await(), node, returnType, parentCount)
                }
            }
            node.children.isNotEmpty() -> node.aliasOrKey toDeferredObj {
                applyObjectProperties(ctx, value, node, returnType, parentCount)
            }
            node is Execution.Union -> node.aliasOrKey toDeferredObj {
                applyObjectProperties(ctx, value, node.memberExecution(returnType), returnType, parentCount)
            }
            else -> node.aliasOrKey toValue createSimpleValueNode(returnType, value, node)
        }
    }

    private fun <T> createSimpleValueNode(returnType: Type, value: T, node: Execution.Node): JsonElement {
        return when (val unwrapped = returnType.unwrapped()) {
            is Type.Scalar<*> -> {
                serializeScalar(unwrapped, value, node)
            }
            is Type.Enum<*> -> JsonPrimitive(value.toString())
            is TypeDef.Object<*> -> throw ExecutionException("Cannot handle object return type, schema structure exception", node)
            else -> throw ExecutionException("Invalid Type:  ${returnType.name}", node)
        }
    }


    private suspend fun <T> DeferredJsonMap.applyObjectProperties(
        ctx: ExecutionContext,
        value: T,
        node: Execution.Node,
        type: Type,
        parentCount: Long
    ) {
        node.children.map { child ->
            when (child) {
                is Execution.Fragment -> handleFragment(ctx, value, child)
                else -> applyProperty(ctx, value, child, type, parentCount)
            }
        }
    }

    private suspend fun <T> DeferredJsonMap.handleFragment(ctx: ExecutionContext, value: T, container: Execution.Fragment) {
        if (!shouldInclude(ctx, container)) return

        val expectedType = container.condition.type

        if (expectedType.kind == TypeKind.OBJECT || expectedType.kind == TypeKind.INTERFACE) {
            if (expectedType.isInstance(value)) {
                container.elements.map { child ->
                    when (child) {
                        is Execution.Fragment -> handleFragment(ctx, value, child)
                        else -> applyProperty(ctx, value, child, expectedType, container.elements.size.toLong())
                    }
                }
            }
        } else if (expectedType.kind == TypeKind.UNION) return handleFragment(
            ctx,
            value,
            container.elements.first { expectedType.name == expectedType.name } as Execution.Fragment
        ) else {
            throw IllegalStateException("fragments can be specified on object types, interfaces, and unions")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> DeferredJsonMap.applyProperty(
        ctx: ExecutionContext,
        value: T,
        child: Execution, type: Type,
        parentCount: Long
    ) {
        when (child) {
            is Execution.Union -> {
                val field = type.unwrapped()[child.key]
                    ?: throw IllegalStateException("Execution unit ${child.key} is not contained by operation return type")
                if (field is Field.Union<*>) {
                    createUnionOperationNode(ctx, value, child, field as Field.Union<T>, parentCount)
                } else {
                    throw ExecutionException("Unexpected non-union field for union execution node", child)
                }
            }
            is Execution.Node -> {
                val field = type.unwrapped()[child.key]
                    ?: throw IllegalStateException("Execution unit ${child.key} is not contained by operation return type")
                createPropertyNodeAsync(ctx, value, child, field, parentCount)
            }
            else -> throw UnsupportedOperationException("Whatever this is isn't supported!")
        }
    }

    private suspend fun <T> DeferredJsonMap.createUnionOperationNode(ctx: ExecutionContext, parent: T, node: Execution.Union, unionProperty: Field.Union<T>, parentCount: Long) {
        node.field.checkAccess(parent, ctx.requestContext)

        val operationResult: Any? = unionProperty.invoke(
            funName = unionProperty.name,
            receiver = parent,
            inputValues = node.field.arguments,
            args = node.arguments,
            executionNode = node,
            ctx = ctx
        )

        val returnType = unionProperty.returnType.possibleTypes.find { it.isInstance(operationResult) }

        if (returnType == null && !unionProperty.nullable) {
            val expectedOneOf = unionProperty.type.possibleTypes!!.joinToString { it.name.toString() }
            throw ExecutionException(
                "Unexpected type of union property value, expected one of: [$expectedOneOf]." +
                        " value was $operationResult", node
            )
        }

        applyKeyToElement(ctx, operationResult, node, returnType ?: unionProperty.returnType, parentCount)
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> DeferredJsonMap.createPropertyNodeAsync(
        ctx: ExecutionContext,
        parentValue: T,
        node: Execution.Node,
        field: Field,
        parentCount: Long
    )  {
        node.field.checkAccess(parentValue, ctx.requestContext)
        if (!shouldInclude(ctx, node)) return


        when (field) {
            is Field.Kotlin<*, *> -> {
                val rawValue = try {
                    (field.kProperty as KProperty1<T, *>).get(parentValue)
                } catch(e: NullPointerException) {
                    throw e
                }
                val value: Any? = field.transformation?.invoke(
                    funName = field.name,
                    receiver = rawValue,
                    inputValues = field.arguments,
                    args = node.arguments,
                    executionNode = node,
                    ctx = ctx
                ) ?: rawValue

                applyKeyToElement(ctx, value, node, field.returnType, parentCount)
            }
            is Field.Function<*, *> -> {
                handleFunctionProperty(ctx, parentValue, node, field, parentCount)
            }
            is Field.DataLoader<*, *, *> -> {
                field as Field.DataLoader<T, *, *>
                handleDataPropertyAsync(ctx, parentValue, node, field, parentCount)
            }
            else -> throw TODO("Only Kotlin Fields are supported!")
        }
    }

    private suspend fun <T> DeferredJsonMap.handleDataPropertyAsync(
        ctx: ExecutionContext,
        parentValue: T,
        node: Execution.Node,
        field: Field.DataLoader<T, *, *>,
        parentCount: Long
    ) {
        val preparedValue = field.kql.prepare.invoke(
            funName = field.name,
            receiver = parentValue,
            inputValues = field.arguments,
            args = node.arguments,
            executionNode = node,
            ctx = ctx
        ) // ?: TODO("Nullable prepare functions isn't supported")


        val value = ctx.loaders[field]!!.loadAsync(preparedValue)


        applyKeyToElement(ctx, value, node, field.returnType, parentCount)
    }

    private suspend fun <T> DeferredJsonMap.handleFunctionProperty(
        ctx: ExecutionContext,
        parentValue: T,
        node: Execution.Node,
        field: Field.Function<*, *>,
        parentCount: Long
    ) {
        val deferred = CompletableDeferred<Any?>()
        deferredLaunch {
            try {
                val res = field.invoke(
                    funName = field.name,
                    receiver = parentValue,
                    inputValues = field.arguments,
                    args = node.arguments,
                    executionNode = node,
                    ctx = ctx
                )
                deferred.complete(res)
            } catch (e: Throwable) {
                deferred.completeExceptionally(e)
            }
        }

        applyKeyToElement(ctx, deferred, node, field.returnType, parentCount)
    }

    override suspend fun suspendExecute(plan: ExecutionPlan, variables: VariablesJson, context: Context) = coroutineScope {
        val result = deferredJsonBuilder(timeout = plan.options.timeout ?: schema.configuration.timeout) {
            val ctx = ExecutionContext(
                Variables(schema, variables, plan.firstOrNull { it.variables != null }?.variables),
                context,
                plan.constructLoaders(),
            )


            "data" toDeferredObj {
                plan.forEach { node ->
                    if (shouldInclude(ctx, node)) writeOperation(ctx, node, node.field as Field.Function<*, *>)
                }
            }
            ctx.loaders.values.map { it.dispatch() }
        }

        result.await().toString()
    }

    private fun createNullNode(node: Execution.Node, returnType: Type): JsonNull = if (returnType !is Type.NonNull) {
        JsonNull
    } else {
        throw ExecutionException("null result for non-nullable operation ${node.field}", node)
    }

    private suspend fun shouldInclude(ctx: ExecutionContext, executionNode: Execution): Boolean {
        if (executionNode.directives?.isEmpty() == true) return true
        return executionNode.directives?.map { (directive, arguments) ->
            directive.execution.invoke(
                funName = directive.name,
                inputValues = directive.arguments,
                receiver = null,
                args = arguments,
                executionNode = executionNode,
                ctx = ctx
            )?.include
                ?: throw ExecutionException("Illegal directive implementation returning null result", executionNode)
        }?.reduce { acc, b -> acc && b } ?: true
    }

    internal suspend operator fun <T> FunctionWrapper<T>.invoke(
        funName: String,
        receiver: Any?,
        inputValues: List<InputValue<*>>,
        args: ArgumentNodes?,
        executionNode: Execution,
        ctx: ExecutionContext
    ): T? {
        val transformedArgs = argumentsHandler.transformArguments(
            funName,
            inputValues,
            args,
            ctx.variables,
            executionNode,
            ctx.requestContext
        )

        return try {
            if (hasReceiver) invoke(receiver, *transformedArgs.toTypedArray())
            else invoke(*transformedArgs.toTypedArray())
        } catch (e: Throwable) {
            if (schema.configuration.wrapErrors && e !is GraphQLError) {
                throw GraphQLError(e.message ?: "", nodes = listOf(executionNode.selectionNode), originalError = e)
            } else throw e
        }
    }
}

