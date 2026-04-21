package com.ismartcoding.lib.kgraphql.schema.execution

class ExecutionPlan (
    val options: ExecutionOptions,
    val operations: List<Execution.Node>
) : List<Execution.Node> by operations {
    var isSubscription = false
}
