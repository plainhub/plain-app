package com.ismartcoding.plain.data

import androidx.compose.ui.unit.IntSize
import kotlinx.datetime.Instant

@kotlinx.serialization.Serializable
data class DVideo(
    override var id: String,
    val title: String,
    override val path: String,
    override val duration: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val bucketId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) : IMedia, IData {

    fun getRotatedSize(): IntSize {
        if (rotation == 90 || rotation == 270) {
            return IntSize(height, width)
        }

        return IntSize(width, height)
    }
}
