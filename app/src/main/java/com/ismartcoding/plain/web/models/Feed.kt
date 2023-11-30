package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.db.DFeed
import kotlinx.datetime.Instant

data class Feed(
    val id: ID,
    val name: String,
    val url: String,
    val fetchContent: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun DFeed.toModel(): Feed {
    return Feed(ID(id), name, url, fetchContent, createdAt, updatedAt)
}
