package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.data.DMediaBucket

data class MediaBucket(
    val id: ID,
    val name: String,
    val itemCount: Int,
    /** Sample media file paths inside the bucket, used for folder thumbnails. */
    val topItemPaths: List<String>,
)

fun DMediaBucket.toModel(): MediaBucket {
    return MediaBucket(ID(id), name, itemCount, topItems)
}
