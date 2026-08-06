package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.helpers.getFileId

fun DFeedEntry.toModel(): FeedEntry {
    return FeedEntry(
        ID(id), title, url, getFileId(image), description, author, content, feedId, rawId, publishedAt, createdAt, updatedAt,
    )
}
