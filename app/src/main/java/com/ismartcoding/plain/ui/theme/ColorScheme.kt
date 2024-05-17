package com.ismartcoding.plain.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.ui.theme.palette.onDark

@Composable
fun ColorScheme.cardContainer(): Color {
    return MaterialTheme.colorScheme.surfaceContainer onDark MaterialTheme.colorScheme.surfaceContainerHighest
}


@Composable
fun ColorScheme.bottomAppBarContainer(): Color {
    return MaterialTheme.colorScheme.surfaceVariant
}


@Composable
fun ColorScheme.lightMask(): Color {
    return Color.White.copy(alpha = 0.4f)
}

@Composable
fun ColorScheme.darkMask(alpha: Float = 0.4f): Color {
    return Color.Black.copy(alpha = alpha)
}