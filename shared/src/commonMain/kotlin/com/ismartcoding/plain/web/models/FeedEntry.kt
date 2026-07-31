package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class FeedEntry(
    val id: ID,
    val title: String,
    val url: String,
    val image: String,
    val description: String,
    val author: String,
    val content: String,
    val feedId: String,
    val rawId: String,
    val publishedAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
)
