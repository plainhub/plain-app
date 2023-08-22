package com.ismartcoding.plain.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.ui.theme.palette.onLight


@Composable
fun ColorScheme.windowBackground(): Color {
    return MaterialTheme.colorScheme.surface onLight MaterialTheme.colorScheme.inverseOnSurface
}