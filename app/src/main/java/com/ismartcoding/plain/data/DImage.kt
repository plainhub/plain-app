package com.ismartcoding.plain.data

import com.ismartcoding.plain.data.IData
import kotlinx.datetime.Instant
import java.io.Serializable

data class DImage(
    override var id: String,
    val title: String,
    val path: String,
    val size: Long,
    val width: Int,
    val height: Int,
    val bucketId: String,
    val takenAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
) : IData, Serializable {
}
