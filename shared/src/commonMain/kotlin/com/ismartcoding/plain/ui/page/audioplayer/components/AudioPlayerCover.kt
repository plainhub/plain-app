package com.ismartcoding.plain.ui.page.audioplayer.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.page.audio.components.audioCoverCache
import com.ismartcoding.plain.platform.loadAudioCoverBitmap

/**
 * Placeholder artwork for tracks without an embedded cover: a music note on a
 * soft disc over a diagonal gradient. Scales with the container so it also
 * reads at header size.
 */
@Composable
private fun DefaultCoverArt(modifier: Modifier = Modifier) {
    val gradientStart = MaterialTheme.colorScheme.primaryContainer
    val gradientEnd = MaterialTheme.colorScheme.tertiaryContainer
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier.background(Brush.linearGradient(listOf(gradientStart, gradientEnd))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val discRadius = size.minDimension * 0.32f
            drawCircle(color = primary.copy(alpha = 0.12f), radius = discRadius)

            // Stroked music note from Res.drawable.music2 (24x24 viewport), centered.
            val s = discRadius * 0.038f
            fun nx(x: Float) = center.x + (x - 11.5f) * s
            fun ny(y: Float) = center.y + (y - 12f) * s
            val note = Path().apply {
                addOval(Rect(nx(4f), ny(14f), nx(12f), ny(22f)))
                moveTo(nx(12f), ny(18f))
                lineTo(nx(12f), ny(2f))
                lineTo(nx(19f), ny(6f))
            }
            drawPath(
                path = note,
                color = primary,
                style = Stroke(width = 2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@Composable
fun AudioPlayerCover(
    path: String,
    modifier: Modifier = Modifier.size(280.dp),
) {
    if (path.isBlank()) {
        DefaultCoverArt(modifier)
        return
    }

    var imageBitmap by remember(path) {
        mutableStateOf<ImageBitmap?>(audioCoverCache[path])
    }
    var isLoading by remember(path) {
        mutableStateOf(!audioCoverCache.containsKey(path))
    }

    LaunchedEffect(path) {
        if (audioCoverCache.containsKey(path)) {
            imageBitmap = audioCoverCache[path]
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        val bitmap = withIO { loadAudioCoverBitmap(path) }

        audioCoverCache[path] = bitmap
        imageBitmap = bitmap
        isLoading = false
    }

    if (imageBitmap != null) {
        Surface(
            modifier = modifier
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.clip(RoundedCornerShape(24.dp))
            )
        }
    } else {
        DefaultCoverArt(modifier)
    }
}
