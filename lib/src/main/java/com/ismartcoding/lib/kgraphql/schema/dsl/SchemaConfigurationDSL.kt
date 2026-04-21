package com.ismartcoding.lib.kgraphql.schema.dsl

import com.ismartcoding.lib.kgraphql.configuration.PluginConfiguration
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ismartcoding.lib.kgraphql.configuration.SchemaConfiguration
import com.ismartcoding.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.lib.kgraphql.schema.execution.GenericTypeResolver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.reflect.KClass

open class SchemaConfigurationDSL {
    var useDefaultPrettyPrinter: Boolean = false
    var useCachingDocumentParser: Boolean = true
    var objectMapper: ObjectMapper = jacksonObjectMapper()
    var documentParserCacheMaximumSize: Long = 1000L
    var acceptSingleValueAsArray: Boolean = true
    var coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
    var wrapErrors: Boolean = true
    var executor: Executor = Executor.Parallel
    var timeout: Long? = null
    var introspection: Boolean = true
    var genericTypeResolver: GenericTypeResolver = GenericTypeResolver.DEFAULT

    private val plugins: MutableMap<KClass<*>, Any> = mutableMapOf()

    fun install(plugin: PluginConfiguration) {
        val kClass = plugin::class
        require(plugins[kClass] == null)
        plugins[kClass] = plugin
    }


    internal fun update(block: SchemaConfigurationDSL.() -> Unit) = block()
    internal fun build(): SchemaConfiguration {
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, acceptSingleValueAsArray)
        return SchemaConfiguration(
            useCachingDocumentParser = useCachingDocumentParser,
            documentParserCacheMaximumSize = documentParserCacheMaximumSize,
            objectMapper = objectMapper,
            useDefaultPrettyPrinter = useDefaultPrettyPrinter,
            coroutineDispatcher = coroutineDispatcher,
            wrapErrors = wrapErrors,
            executor = executor,
            timeout = timeout,
            introspection = introspection,
            plugins = plugins,
            genericTypeResolver = genericTypeResolver,
        )
    }
}
