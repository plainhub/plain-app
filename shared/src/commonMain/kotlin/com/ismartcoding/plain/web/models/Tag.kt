package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlinx.serialization.Serializable

@GraphQLType
@Serializable
data class Tag(
    var id: ID,
    var name: String,
    var count: Int,
)

fun DTag.toModel(): Tag {
    return Tag(ID(id), name, count)
}
