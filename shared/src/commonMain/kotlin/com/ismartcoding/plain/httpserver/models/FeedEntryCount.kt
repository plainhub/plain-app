package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.DFeedCount

data class FeedEntryCount(val id: String, val count: Int)

fun DFeedCount.toModel(): FeedEntryCount {
    return FeedEntryCount(id, entryCount)
}