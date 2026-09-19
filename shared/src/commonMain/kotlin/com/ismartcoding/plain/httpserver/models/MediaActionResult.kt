package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.enums.MediaDataType
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType

@GraphQLType
data class MediaActionResult(
    val type: MediaDataType,
    val query: String,
    val affectedCount: Int,
)
