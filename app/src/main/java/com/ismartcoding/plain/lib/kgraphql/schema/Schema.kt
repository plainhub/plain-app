package com.ismartcoding.plain.lib.kgraphql.schema

import com.ismartcoding.plain.lib.kgraphql.Context
import com.ismartcoding.plain.lib.kgraphql.configuration.SchemaConfiguration
import com.ismartcoding.plain.lib.kgraphql.schema.execution.ExecutionOptions
import com.ismartcoding.plain.lib.kgraphql.schema.introspection.__Schema
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language

interface Schema : __Schema {
    val configuration: SchemaConfiguration

    suspend fun execute(
        @Language("graphql") request: String,
        variables: String? = null,
        context: Context = Context(emptyMap()),
        options: ExecutionOptions = ExecutionOptions(),
        operationName: String? = null
    ): String

    fun executeBlocking(
        @Language("graphql") request: String,
        variables: String? = null,
        context: Context = Context(emptyMap()),
        options: ExecutionOptions = ExecutionOptions(),
        operationName: String? = null,
    ) = runBlocking { execute(request, variables, context, options, operationName) }
}
