package com.ismartcoding.lib.kgraphql.helpers

import com.ismartcoding.lib.kgraphql.schema.execution.Execution

/**
 * This returns a list of all scalar fields requested on this type.
 */
fun Execution.getFields(): List<String> = when (this) {
    is Execution.Fragment -> elements.flatMap(Execution::getFields)
    is Execution.Node -> {
        if (children.isEmpty()) listOf(key)
        else children
            .filterNot { (it is Execution.Node && it.children.isNotEmpty()) }
            .flatMap(Execution::getFields)

    }
    else -> listOf()
}.distinct()
