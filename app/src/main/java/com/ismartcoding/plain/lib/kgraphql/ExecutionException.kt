package com.ismartcoding.plain.lib.kgraphql

import com.ismartcoding.plain.lib.kgraphql.schema.execution.Execution
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.ASTNode

class ExecutionException(
    message: String,
    node: ASTNode? = null,
    cause: Throwable? = null
) : GraphQLError(
    message,
    nodes = node?.let(::listOf),
    originalError = cause
) {
    constructor(message: String, node: Execution, cause: Throwable? = null): this(message, node.selectionNode, cause)
}
