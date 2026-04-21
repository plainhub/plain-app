package com.ismartcoding.lib.kgraphql.configuration

import com.ismartcoding.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.lib.kgraphql.schema.execution.GenericTypeResolver
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.reflect.KClass

data class SchemaConfiguration(
        //document parser caching mechanisms
        val useCachingDocumentParser: Boolean,
        val documentParserCacheMaximumSize: Long,
        //jackson features
        val objectMapper: ObjectMapper,
        val useDefaultPrettyPrinter: Boolean,
        //execution
        val coroutineDispatcher: CoroutineDispatcher,

        val wrapErrors: Boolean,

        val executor: Executor,
        val timeout: Long?,
        val introspection: Boolean = true,
        val plugins: MutableMap<KClass<*>, Any>,

        val genericTypeResolver: GenericTypeResolver,
) {
        @Suppress("UNCHECKED_CAST")
        operator fun <T: Any> get(type: KClass<T>) = plugins[type] as T?
}
