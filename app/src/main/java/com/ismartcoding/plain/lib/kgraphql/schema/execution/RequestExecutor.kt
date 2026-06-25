package com.ismartcoding.plain.lib.kgraphql.schema.execution

import com.ismartcoding.plain.lib.kgraphql.Context
import com.ismartcoding.plain.lib.kgraphql.request.VariablesJson


interface RequestExecutor {
    suspend fun suspendExecute(plan : ExecutionPlan, variables: VariablesJson, context: Context): String
}
