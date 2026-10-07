package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.DTagRelation
import kotlinx.serialization.Serializable

@Serializable
data class TagRelation(
    var tagId: ID = ID(""),
    var key: String = "",
)

fun DTagRelation.toModel(): TagRelation {
    return TagRelation(ID(tagId), key)
}
