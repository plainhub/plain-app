package com.ismartcoding.plain.data

import android.os.Parcelable
import androidx.compose.ui.unit.IntSize
import kotlinx.datetime.Instant
import kotlinx.parcelize.Parcelize
import java.io.Serializable

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
) : IData, IMedia, Serializable {

    fun getRotatedSize(): IntSize {
        if (rotation == 90 || rotation == 270) {
            return IntSize(height, width)
        }

        return IntSize(width, height)
    }
}
