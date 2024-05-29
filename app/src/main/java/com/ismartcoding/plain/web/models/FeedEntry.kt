package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.helpers.FileHelper
import kotlinx.datetime.Instant

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

fun DFeedEntry.toModel(): FeedEntry {
    return FeedEntry(
        ID(id), title, url, FileHelper.getFileId(image), description, author, content, feedId, rawId, publishedAt, createdAt, updatedAt,
    )
}
