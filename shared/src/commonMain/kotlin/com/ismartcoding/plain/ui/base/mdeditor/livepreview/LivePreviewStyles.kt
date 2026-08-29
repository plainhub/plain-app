package com.ismartcoding.plain.ui.base.mdeditor.livepreview

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

data class LivePreviewStyles(
    val headingColor: Color,
    val linkColor: Color,
    val codeTextColor: Color,
    val codeBackground: Color,
    val quoteColor: Color,
    val markerColor: Color,
    val mathColor: Color,
    val highlightBackground: Color,
    val imageChipColor: Color,
    val imageChipBackground: Color,
    val monospace: FontFamily,
)

@Composable
fun rememberLivePreviewStyles(): LivePreviewStyles {
    val colorScheme = MaterialTheme.colorScheme
    return remember(colorScheme) {
        LivePreviewStyles(
            headingColor = colorScheme.onSurface,
            linkColor = colorScheme.primary,
            codeTextColor = colorScheme.onSurface,
            codeBackground = colorScheme.surfaceVariant,
            quoteColor = colorScheme.onSurfaceVariant,
            markerColor = colorScheme.onSurfaceVariant,
            mathColor = colorScheme.tertiary,
            highlightBackground = colorScheme.secondaryContainer,
            imageChipColor = colorScheme.onPrimaryContainer,
            imageChipBackground = colorScheme.primaryContainer,
            monospace = FontFamily.Monospace,
        )
    }
}
