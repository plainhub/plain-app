package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.db.DFeedCount

data class FeedCount(val id: String, val count: Int)

fun DFeedCount.toModel(): FeedCount {
    return FeedCount(id, count)
}