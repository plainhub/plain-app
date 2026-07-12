package com.ismartcoding.plain.ui.components.mediaviewer.video

import androidx.compose.ui.unit.IntSize

/**
 * Pure computation for video sizing within a container.
 */
data class MediaVideoSizing(
    val containerSize: IntSize,
    val videoIntrinsicSize: IntSize,
) {
    private val containerRatio get() = containerSize.width.toFloat() / containerSize.height.toFloat()
    val videoRatio get() = if (videoIntrinsicSize.height == 0) 1f else videoIntrinsicSize.width.toFloat() / videoIntrinsicSize.height.toFloat()
    val widthFixed get() = videoRatio > containerRatio
    val superSize get() = videoIntrinsicSize.height > containerSize.height && videoIntrinsicSize.width > containerSize.width

    val displaySize: IntSize
        get() {
            if (videoIntrinsicSize == IntSize.Zero || containerSize == IntSize.Zero) return containerSize
            return if (widthFixed) {
                val w = containerSize.width
                IntSize(w, (w / videoRatio).toInt())
            } else {
                val h = containerSize.height
                IntSize((h * videoRatio).toInt(), h)
            }
        }

    fun renderedSize(scale: Float) = IntSize(
        (displaySize.width * scale).toInt(),
        (displaySize.height * scale).toInt()
    )

    val maxScale: Float
        get() = when {
            superSize -> videoIntrinsicSize.width.toFloat() / displaySize.width.toFloat()
            widthFixed -> containerSize.height.toFloat() / displaySize.height.toFloat()
            else -> containerSize.width.toFloat() / displaySize.width.toFloat()
        }
}
