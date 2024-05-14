package com.ismartcoding.plain.ui.base.videoplayer

import androidx.annotation.OptIn
import androidx.compose.runtime.Stable
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout

@Stable
enum class ResizeMode(val value: String) {
    /**
     * Either the width or height is decreased to obtain the desired aspect ratio.
     */
    FIT("fit"),

    /**
     * The width is fixed and the height is increased or decreased to obtain the desired aspect ratio.
     */
    FIXED_WIDTH("fixed_width"),

    /**
     * The height is fixed and the width is increased or decreased to obtain the desired aspect ratio.
     */
    FIXED_HEIGHT("fixed_height"),

    /**
     * The specified aspect ratio is ignored.
     */
    FILL("fill"),

    /**
     * Either the width or height is increased to obtain the desired aspect ratio.
     */
    ZOOM("zoom"),
}

/**
 * Convert [ResizeMode] to playerview resize mode.
 *
 * @return [AspectRatioFrameLayout.RESIZE_MODE_FIT] or [AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH]
 * or [AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT] or [AspectRatioFrameLayout.RESIZE_MODE_FILL]
 * or [AspectRatioFrameLayout.RESIZE_MODE_ZOOM]
 */
@OptIn(UnstableApi::class)
internal fun ResizeMode.toPlayerViewResizeMode(): Int =
    when (this) {
        ResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        ResizeMode.FIXED_WIDTH -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
        ResizeMode.FIXED_HEIGHT -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
        ResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        ResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    }

/**
 * Convert playerview resize mode to [ResizeMode].
 *
 * @return [ResizeMode.FIT] or [ResizeMode.FIXED_WIDTH] or [ResizeMode.FIXED_HEIGHT]
 * or [ResizeMode.FILL] or [ResizeMode.ZOOM]
 */
fun Int.toResizeMode(): ResizeMode =
    if (this in 0 until 5) {
        when (this) {
            0 -> ResizeMode.FIT
            1 -> ResizeMode.FIXED_WIDTH
            2 -> ResizeMode.FIXED_HEIGHT
            3 -> ResizeMode.FILL
            4 -> ResizeMode.ZOOM
            else -> throw IllegalStateException("This is not PlayerView resize mode.")
        }
    } else {
        throw IllegalStateException("This is not PlayerView resize mode.")
    }
