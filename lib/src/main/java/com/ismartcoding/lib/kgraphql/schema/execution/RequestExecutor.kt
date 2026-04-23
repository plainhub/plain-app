package com.ismartcoding.lib.kgraphql.schema.execution

import com.ismartcoding.lib.kgraphql.Context
import com.ismartcoding.lib.kgraphql.request.VariablesJson


interface RequestExecutor {
    suspend fun suspendExecute(plan : ExecutionPlan, variables: VariablesJson, context: Context): String
}
