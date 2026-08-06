package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlinx.serialization.Serializable

@GraphQLType
@Serializable
data class TagRelation(
    var tagId: String = "",
    var key: String = "",
)

fun DTagRelation.toModel(): TagRelation {
    return TagRelation(tagId, key)
}
