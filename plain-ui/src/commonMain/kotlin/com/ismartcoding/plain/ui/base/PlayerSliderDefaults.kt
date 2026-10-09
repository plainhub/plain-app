package com.ismartcoding.plain.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object PlayerSliderDefaults {
    /** Colors suited for dark overlays (video players). Used as the slider default. */
    val darkColors: PlayerSliderColors = PlayerSliderColors(
        trackColor = Color.DarkGray.copy(alpha = 0.4f),
        bufferColor = Color.Gray,
        progressColor = Color.White,
        thumbColor = Color.White,
    )

    /** Colors derived from MaterialTheme for light surfaces. Call inside @Composable. */
    @Composable
    fun lightColors(): PlayerSliderColors = PlayerSliderColors(
        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
        bufferColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
        progressColor = MaterialTheme.colorScheme.primary,
        thumbColor = MaterialTheme.colorScheme.primary,
    )
}
