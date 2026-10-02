package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class Feed(
    val id: ID,
    val name: String,
    val url: String,
    val fetchContent: Boolean,
    val logo: String,
    val lastSyncAt: Instant?,
    val lastError: FeedError,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun DFeed.toModel(): Feed {
    return Feed(ID(id), name, url, fetchContent, com.ismartcoding.plain.helpers.getFileId(logo), lastSyncAt, FeedError(lastError.code, lastError.detail), createdAt, updatedAt)
}
