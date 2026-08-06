package com.ismartcoding.plain.lib.markdown.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.Density

interface ImageTransformer {
    /**
     * Will retrieve the [ImageData] from an image link/url
     */
    @Composable
    fun transform(link: String): ImageData?

    /**
     * Returns the detected intrinsic size of the painter
     */
    @Composable
    fun intrinsicSize(painter: Painter): Size {
        return painter.intrinsicSize
    }

    /**
     * The expected placeholderSize for an inline image identified by [link].
     */
    fun placeholderConfig(
        link: String,
        density: Density,
        containerSize: Size,
        imageWidth: ImageWidth,
        imageSize: Size,
        imageSizeChanged: ((link: String, Size) -> Unit)? = null,
    ): PlaceholderConfig {
        fun scale(size: Size, factor: Float): Size {
            return Size(size.width * factor, size.height * factor)
        }

        val sizeInDp = with(density) {
            if (containerSize.isUnspecified) {
                // No container size known, use default
                Size(DEFAULT_IMAGE_SIDE_LENGTH_DP, DEFAULT_IMAGE_SIDE_LENGTH_DP)
            } else if (imageSize.isUnspecified) {
                // No image size known yet, use container-constrained max side
                val containerWidthDp = containerSize.width.toDp().value
                val containerHeightDp = containerSize.height.toDp().value
                val actualSideLength = minOf(DEFAULT_IMAGE_SIDE_LENGTH_DP, containerHeightDp, containerWidthDp)
                Size(actualSideLength, actualSideLength)
            } else {
                // Both sizes known, calculate scaled size to fit container
                val imageWidthDp = imageSize.width.toDp().value
                val imageHeightDp = imageSize.height.toDp().value
                val containerWidthDp = containerSize.width.toDp().value
                val containerHeightDp = containerSize.height.toDp().value

                val containerCappedHeight = minOf(imageHeightDp, containerHeightDp)
                val containerCappedWidth = when (imageWidth) {
                    ImageWidth.IMAGE_WIDTH -> minOf(imageWidthDp, containerWidthDp)
                    ImageWidth.MAX_WIDTH -> containerWidthDp
                }

                if (containerCappedWidth < imageWidthDp || containerCappedHeight < imageHeightDp) {
                    // Image needs scaling down to fit
                    when (imageWidth) {
                        ImageWidth.IMAGE_WIDTH -> {
                            val lowestRatio = minOf(containerCappedWidth / imageWidthDp, containerCappedHeight / imageHeightDp)
                            scale(Size(imageWidthDp, imageHeightDp), lowestRatio)
                        }
                        ImageWidth.MAX_WIDTH -> {
                            val widthRatio = containerCappedWidth / imageWidthDp
                            Size(containerCappedWidth, imageHeightDp * widthRatio)
                        }
                    }
                } else {
                    // Image fits naturally in container; for MAX_WIDTH scale up to fill width proportionally
                    when (imageWidth) {
                        ImageWidth.IMAGE_WIDTH -> Size(containerCappedWidth, containerCappedHeight)
                        ImageWidth.MAX_WIDTH -> {
                            val widthRatio = containerCappedWidth / imageWidthDp
                            Size(containerCappedWidth, imageHeightDp * widthRatio)
                        }
                    }
                }
            }
        }

        return PlaceholderConfig(sizeInDp)
    }

    companion object {
        const val DEFAULT_IMAGE_SIDE_LENGTH_DP = 200f
    }
}

@Immutable
data class PlaceholderConfig(
    val size: Size,
    val verticalAlign: PlaceholderVerticalAlign = PlaceholderVerticalAlign.Bottom,
)

@Immutable
data class ImageData(
    val painter: Painter,
    val modifier: Modifier = Modifier,
    val contentDescription: String? = "Image",
    val alignment: Alignment = Alignment.CenterStart,
    val contentScale: ContentScale = ContentScale.Fit,
    val alpha: Float = DefaultAlpha,
    val colorFilter: ColorFilter? = null,
    /**
     * Optional tap handler. When non-null, the rendered image is wrapped in
     * a `Modifier.clickable` that invokes this callback on click. The
     * callback receives no arguments — the image's destination URL is
     * already available to the transformer that built this [ImageData]
     * (via [contentDescription] or via closure capture).
     */
    val onClick: (() -> Unit)? = null,
)
