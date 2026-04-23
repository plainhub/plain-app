package com.ismartcoding.lib.kgraphql.schema.execution

import com.ismartcoding.lib.kgraphql.configuration.SchemaConfiguration

/**
 * If fields are null it'll fallback to the default from [SchemaConfiguration].
 */
data class ExecutionOptions(
    val executor: Executor? = null,
    val timeout: Long? = null
)
