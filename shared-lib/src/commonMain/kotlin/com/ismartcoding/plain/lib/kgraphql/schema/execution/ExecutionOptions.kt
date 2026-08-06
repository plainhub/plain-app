package com.ismartcoding.plain.lib.kgraphql.schema.execution

import com.ismartcoding.plain.lib.kgraphql.configuration.SchemaConfiguration

/**
 * If fields are null it'll fallback to the default from [com.ismartcoding.plain.lib.kgraphql.configuration.SchemaConfiguration].
 */
data class ExecutionOptions(
    val executor: Executor? = null,
    val timeout: Long? = null
)
