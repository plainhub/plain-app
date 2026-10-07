package com.ismartcoding.plain.httpserver.models

import kotlin.time.Instant

data class Doc(
    override val id: ID,
    override val title: String,
    override val path: String,
    val extension: String,
    override val size: Long,
    override val bucketId: ID,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : MediaItem
