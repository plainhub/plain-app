package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.db.DFeedCount
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType

@GraphQLType
data class FeedCount(val id: String, val count: Int)

fun DFeedCount.toModel(): FeedCount {
    return FeedCount(id, count)
}