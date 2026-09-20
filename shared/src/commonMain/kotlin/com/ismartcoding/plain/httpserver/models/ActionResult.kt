package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType

/** Result of a synchronous bulk mutation: how many items were affected. */
@GraphQLType
data class ActionResult(
    val affectedCount: Int,
)
