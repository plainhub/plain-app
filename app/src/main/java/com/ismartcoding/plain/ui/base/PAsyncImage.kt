package com.ismartcoding.plain.ui.base

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
import com.ismartcoding.plain.R

@Composable
fun PAsyncImage(
    modifier: Modifier = Modifier,
    data: Any? = null,
    size: Size = Size.ORIGINAL,
    scale: Scale = Scale.FIT,
    precision: Precision = Precision.AUTOMATIC,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String = "",
    @DrawableRes placeholder: Int? = R.drawable.ic_hourglass_empty,
    @DrawableRes error: Int? = R.drawable.ic_broken_image,
) {
    Image(
        painter =
            rememberAsyncImagePainter(
                model =
                    ImageRequest.Builder(LocalContext.current).data(data)
                        .videoFrameMillis(3000)
                        .apply {
                            if (placeholder != null) placeholder(placeholder)
                            if (error != null) error(error)
                            crossfade(true)
                            scale(scale)
                            precision(precision)
                            size(size)
                        }.build(),
            ),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}

// From: https://gist.github.com/colinrtwhite/c2966e0b8584b4cdf0a5b05786b20ae1

/**
 * Create and return a new [Painter] that wraps [painter] with its [alpha], [colorFilter], or [onDraw] overwritten.
 */
fun forwardingPainter(
    painter: Painter,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    onDraw: DrawScope.(ForwardingDrawInfo) -> Unit = DefaultOnDraw,
): Painter = ForwardingPainter(painter, alpha, colorFilter, onDraw)

@Immutable
data class ForwardingDrawInfo(
    val painter: Painter,
    val alpha: Float,
    val colorFilter: ColorFilter?,
)

@Immutable
private class ForwardingPainter(
    private val painter: Painter,
    private var alpha: Float,
    private var colorFilter: ColorFilter?,
    private val onDraw: DrawScope.(ForwardingDrawInfo) -> Unit,
) : Painter() {
    private var info = newInfo()

    override val intrinsicSize get() = painter.intrinsicSize

    override fun applyAlpha(alpha: Float): Boolean {
        if (alpha == DefaultAlpha) {
            this.alpha = alpha
            this.info = newInfo()
        }
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        if (colorFilter == null) {
            this.info = newInfo()
        }
        return true
    }

    override fun DrawScope.onDraw() = onDraw(info)

    private fun newInfo() = ForwardingDrawInfo(painter, alpha, colorFilter)
}

private val DefaultOnDraw: DrawScope.(ForwardingDrawInfo) -> Unit = { info ->
    with(info.painter) {
        draw(
            androidx.compose.ui.geometry.Size(size.width, size.height),
            info.alpha,
            info.colorFilter,
        )
    }
}
