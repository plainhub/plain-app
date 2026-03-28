package com.ismartcoding.plain.ui.components.mediaviewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage

@Composable
fun MediaNormalImageContent(
    imageModifier: Modifier,
    painter: Painter?,
    model: PreviewItem,
    uSize: IntSize,
) {
    if (painter != null) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = imageModifier.size(
                LocalDensity.current.run { uSize.width.toDp() },
                LocalDensity.current.run { uSize.height.toDp() }
            ),
        )
    } else {
        if (model.path.endsWith(".svg", true)) {
            AsyncImage(
                model = model.path,
                contentDescription = model.path,
                contentScale = ContentScale.Fit,
                modifier = imageModifier
                    .background(Color.White)
                    .size(
                        LocalDensity.current.run { uSize.width.toDp() },
                        LocalDensity.current.run { uSize.height.toDp() }
                    ),
            )
        } else {
            AsyncImage(
                model = model.path,
                contentDescription = model.path,
                contentScale = ContentScale.Fit,
                modifier = imageModifier.fillMaxSize(),
            )
        }
    }
}
