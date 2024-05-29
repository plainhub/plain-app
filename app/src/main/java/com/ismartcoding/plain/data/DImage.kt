package com.ismartcoding.plain.data

import androidx.compose.ui.unit.IntSize
import kotlinx.datetime.Instant


data class DImage(
    override var id: String,
    val title: String,
    val path: String,
    val size: Long,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val bucketId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) : IData {
    fun getRotatedSize(): IntSize {
        if (rotation == 90 || rotation == 270) {
            return IntSize(height, width)
        }

        return IntSize(width, height)
    }
}
