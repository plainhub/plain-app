package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class Feed(
    val id: ID,
    val name: String,
    val url: String,
    val fetchContent: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun DFeed.toModel(): Feed {
    return Feed(ID(id), name, url, fetchContent, createdAt, updatedAt)
}
