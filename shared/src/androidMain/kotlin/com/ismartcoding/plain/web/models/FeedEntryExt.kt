package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.helpers.FileHelper

fun DFeedEntry.toModel(): FeedEntry {
    return FeedEntry(
        ID(id), title, url, FileHelper.getFileId(image), description, author, content, feedId, rawId, publishedAt, createdAt, updatedAt,
    )
}
