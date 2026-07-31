package com.ismartcoding.ksp.kgraphql

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * Service-loaded entry point for the KGraphQL KSP2 processor.
 * Registered in META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider.
 */
class GraphQLSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return GraphQLSymbolProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
        )
    }
}
