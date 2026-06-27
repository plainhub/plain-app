package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.db.DTagRelation
import kotlinx.serialization.Serializable

@Serializable
data class TagRelation(
    var tagId: String = "",
    var key: String = "",
)

fun DTagRelation.toModel(): TagRelation {
    return TagRelation(tagId, key)
}
