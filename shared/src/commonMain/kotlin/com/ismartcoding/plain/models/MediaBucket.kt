package com.ismartcoding.plain.models

import com.ismartcoding.plain.IData
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class MediaBucket(
    override var id: String,
    var name: String,
    var itemCount: Int,
    var coverItems: List<MediaBucketCover>,
    var size: Long = 0,
) : IData

@Serializable
data class MediaBucketCover(val path: String, val updatedAt: Instant, val thumbHash: String?)
