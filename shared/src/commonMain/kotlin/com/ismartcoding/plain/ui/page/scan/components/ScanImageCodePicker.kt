package com.ismartcoding.plain.ui.page.scan.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import coil3.compose.AsyncImage
import com.ismartcoding.plain.platform.ScannedCode
import com.ismartcoding.plain.platform.ScannedImage
import kotlin.math.roundToInt

/** Aspect-fit rect of an image displayed inside a view, in px. */
data class FitRect(val left: Float, val top: Float, val width: Float, val height: Float)

object ScanImageLayout {
    /** ContentScale.Fit rect: the image is scaled to fit entirely, centered in the view. */
    fun fitRect(viewWidth: Float, viewHeight: Float, imageWidth: Float, imageHeight: Float): FitRect {
        if (viewWidth <= 0f || viewHeight <= 0f || imageWidth <= 0f || imageHeight <= 0f) {
            return FitRect(0f, 0f, viewWidth, viewHeight)
        }
        val scale = minOf(viewWidth / imageWidth, viewHeight / imageHeight)
        val w = imageWidth * scale
        val h = imageHeight * scale
        return FitRect((viewWidth - w) / 2f, (viewHeight - h) / 2f, w, h)
    }

    /**
     * Maps an image-normalized point (0..1) into view px inside the fitted rect, clamped
     * so the tag pinned on it stays fully inside the view.
     */
    fun tagCenterIn(
        normalizedX: Float,
        normalizedY: Float,
        rect: FitRect,
        viewWidth: Float,
        viewHeight: Float,
        tagRadius: Float,
    ): Pair<Float, Float> {
        val x = rect.left + normalizedX * rect.width
        val y = rect.top + normalizedY * rect.height
        val cx = x.coerceIn(tagRadius, (viewWidth - tagRadius).coerceAtLeast(tagRadius))
        val cy = y.coerceIn(tagRadius, (viewHeight - tagRadius).coerceAtLeast(tagRadius))
        return Pair(cx, cy)
    }
}

/**
 * WeChat-style picker for a picked image containing several codes: the image is shown
 * aspect-fit with a green tag on every code; tapping a tag opens that code.
 */
@Composable
fun ScanImageCodePicker(
    uri: String,
    image: ScannedImage,
    onPick: (ScannedCode) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val viewW = with(density) { maxWidth.toPx() }
            val viewH = with(density) { maxHeight.toPx() }
            val tagRadius = scanCodeTagRadiusPx(density)
            val rect = ScanImageLayout.fitRect(viewW, viewH, image.width.toFloat(), image.height.toFloat())
            image.codes.forEachIndexed { index, code ->
                val (cx, cy) = ScanImageLayout.tagCenterIn(code.centerX, code.centerY, rect, viewW, viewH, tagRadius)
                ScanCodeTag(
                    index = index,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset { IntOffset((cx - tagRadius).roundToInt(), (cy - tagRadius).roundToInt()) },
                    onPick = { onPick(code) },
                )
            }
        }
    }
}
