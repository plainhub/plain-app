package com.ismartcoding.plain.lib.markdown.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter

class NoOpImageTransformerImpl : ImageTransformer {

    @Composable
    override fun transform(link: String): ImageData? {
        return null
    }

    @Composable
    override fun intrinsicSize(painter: Painter): Size {
        return painter.intrinsicSize
    }
}