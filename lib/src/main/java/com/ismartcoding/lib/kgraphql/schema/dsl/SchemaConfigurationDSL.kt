package com.ismartcoding.lib.kgraphql.schema.dsl

import com.ismartcoding.lib.kgraphql.configuration.PluginConfiguration
import com.ismartcoding.lib.kgraphql.configuration.SchemaConfiguration
import com.ismartcoding.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.lib.kgraphql.schema.execution.GenericTypeResolver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.JsonElement
import kotlin.reflect.KClass

open class SchemaConfigurationDSL {
    var useDefaultPrettyPrinter: Boolean = false
    var useCachingDocumentParser: Boolean = true
    var documentParserCacheMaximumSize: Long = 1000L
    var coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
    var wrapErrors: Boolean = true
    var executor: Executor = Executor.Parallel
    var timeout: Long? = null
    var introspection: Boolean = true
    var genericTypeResolver: GenericTypeResolver = GenericTypeResolver.DEFAULT

    @PublishedApi
    internal val scalarDeserializers: MutableMap<KClass<*>, (JsonElement) -> Any?> = mutableMapOf()
    private val plugins: MutableMap<KClass<*>, Any> = mutableMapOf()

    fun install(plugin: PluginConfiguration) {
        val kClass = plugin::class
        require(plugins[kClass] == null)
        plugins[kClass] = plugin
    }


    internal fun update(block: SchemaConfigurationDSL.() -> Unit) = block()
    internal fun build(): SchemaConfiguration {
        return SchemaConfiguration(
            useCachingDocumentParser = useCachingDocumentParser,
            documentParserCacheMaximumSize = documentParserCacheMaximumSize,
            scalarDeserializers = scalarDeserializers,
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
