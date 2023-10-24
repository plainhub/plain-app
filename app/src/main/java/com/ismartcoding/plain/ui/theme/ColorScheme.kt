package com.ismartcoding.plain.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.ui.theme.palette.onDark
import com.ismartcoding.plain.ui.theme.palette.onLight

@Composable
fun ColorScheme.canvas(): Color {
    return MaterialTheme.colorScheme.surface onLight MaterialTheme.colorScheme.inverseOnSurface
}

@Composable
fun ColorScheme.cardBack(): Color {
    return MaterialTheme.colorScheme.surface onDark MaterialTheme.colorScheme.inverseOnSurface
}

fun ColorScheme.warning(): Color {
    return Color(android.graphics.Color.parseColor("#FFC107"))
}

fun ColorScheme.green(): Color {
    return Color(android.graphics.Color.parseColor("#4CAF50"))
}
