package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.platform.getFileId

fun DFeedEntry.toModel(): FeedEntry {
    return FeedEntry(
        ID(id), title, url, getFileId(image), description, author, content, feedId, rawId, publishedAt, createdAt, updatedAt,
    )
}
