package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DMediaBucket

data class MediaBucket(val id: ID, val name: String, val itemCount: Int, val topItems: MutableList<String>)

fun DMediaBucket.toModel(): MediaBucket {
    return MediaBucket(ID(id), name, itemCount, topItems)
}