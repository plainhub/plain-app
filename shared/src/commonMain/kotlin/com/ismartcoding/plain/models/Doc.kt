package com.ismartcoding.plain.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Doc(
    override var id: String,
    override var title: String,
    override var path: String,
    var extension: String = "",
    override var size: Long,
    override var bucketId: String,
    override var createdAt: Instant,
    override var updatedAt: Instant,
) : MediaItem
