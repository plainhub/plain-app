package com.ismartcoding.lib.kgraphql

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject


@Serializable
data class GraphqlRequest(
    val operationName: String? = null,
    val variables: JsonObject? = null,
    val query: String
)
