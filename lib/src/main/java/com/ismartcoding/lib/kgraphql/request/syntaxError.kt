package com.ismartcoding.lib.kgraphql.request

import com.ismartcoding.lib.kgraphql.GraphQLError
import com.ismartcoding.lib.kgraphql.schema.model.ast.Source

internal fun syntaxError(
    source: Source,
    position: Int,
    description: String
) = GraphQLError(
    message = "Syntax Error: $description",
    nodes = null,
    source = source,
    positions = listOf(position)
)
