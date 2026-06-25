package com.ismartcoding.plain.lib.kgraphql.schema

import com.ismartcoding.plain.lib.kgraphql.Context
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.configuration.SchemaConfiguration
import com.ismartcoding.plain.lib.kgraphql.request.Parser
import com.ismartcoding.plain.lib.kgraphql.request.VariablesJson
import com.ismartcoding.plain.lib.kgraphql.schema.execution.*
import com.ismartcoding.plain.lib.kgraphql.schema.execution.Executor.DataLoaderPrepared
import com.ismartcoding.plain.lib.kgraphql.schema.execution.Executor.Parallel
import com.ismartcoding.plain.lib.kgraphql.schema.introspection.__Schema
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.NameNode
import com.ismartcoding.plain.lib.kgraphql.schema.structure.LookupSchema
import com.ismartcoding.plain.lib.kgraphql.schema.structure.RequestInterpreter
import com.ismartcoding.plain.lib.kgraphql.schema.structure.SchemaModel
import com.ismartcoding.plain.lib.kgraphql.schema.structure.Type
import com.ismartcoding.plain.lib.kgraphql.schema.execution.DataLoaderPreparedRequestExecutor
import com.ismartcoding.plain.lib.kgraphql.schema.execution.ExecutionOptions
import com.ismartcoding.plain.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.plain.lib.kgraphql.schema.execution.ParallelRequestExecutor
import com.ismartcoding.plain.lib.kgraphql.schema.execution.RequestExecutor
import kotlinx.coroutines.coroutineScope
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

class DefaultSchema(
    override val configuration: SchemaConfiguration,
    internal val model: SchemaModel
) : Schema, __Schema by model, LookupSchema {

    companion object {
        val OPERATION_NAME_PARAM = NameNode("operationName", null)
    }

    private val defaultRequestExecutor: RequestExecutor = getExecutor(configuration.executor)

    private fun getExecutor(executor: Executor) = when (executor) {
        Executor.Parallel -> ParallelRequestExecutor(this)
        Executor.DataLoaderPrepared -> DataLoaderPreparedRequestExecutor(this)
    }

    private val requestInterpreter: RequestInterpreter = RequestInterpreter(model)

    override suspend fun execute(
        request: String,
        variables: String?,
        context: Context,
        options: ExecutionOptions,
        operationName: String?,
    ): String = coroutineScope {
        val parsedVariables = variables
            ?.let { VariablesJson.Defined(variables, configuration.scalarDeserializers) }
            ?: VariablesJson.Empty()

        if (!configuration.introspection && request.isIntrospection()) {
            throw GraphQLError("GraphQL introspection is not allowed")
        }

        val document = Parser(request).parseDocument()

        val executor = options.executor?.let(this@DefaultSchema::getExecutor) ?: defaultRequestExecutor

        executor.suspendExecute(
            plan = requestInterpreter.createExecutionPlan(document, operationName, parsedVariables, options),
            variables = parsedVariables,
            context = context
        )
    }

    private fun String.isIntrospection() = this.contains("__schema") || this.contains("__type")

    override fun typeByKClass(kClass: KClass<*>): Type? = model.queryTypes[kClass]

    override fun typeByKType(kType: KType): Type? = typeByKClass(kType.jvmErasure)

    override fun inputTypeByKClass(kClass: KClass<*>): Type? = model.inputTypes[kClass]

    override fun inputTypeByKType(kType: KType): Type? = typeByKClass(kType.jvmErasure)

    override fun typeByName(name: String): Type? = model.queryTypesByName[name]

    override fun inputTypeByName(name: String): Type? = model.inputTypesByName[name]
}
