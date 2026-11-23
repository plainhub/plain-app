package com.ismartcoding.plain.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Image(
    override var id: String,
    override var title: String,
    override var path: String,
    override var size: Long,
    override var bucketId: String,
    override var createdAt: Instant,
    override var updatedAt: Instant,
    var takenAt: Instant?,
    var isFavorite: Boolean = false,
    var width: Int = 0,
    var height: Int = 0,
    var rotation: Int = 0,
    val thumbHash: String? = null,
) : MediaItem
