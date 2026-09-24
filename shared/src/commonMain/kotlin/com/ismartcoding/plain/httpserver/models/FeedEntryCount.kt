package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.DFeedCount
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType

@GraphQLType
data class FeedEntryCount(val id: String, val count: Int)

fun DFeedCount.toModel(): FeedEntryCount {
    return FeedEntryCount(id, entryCount)
}