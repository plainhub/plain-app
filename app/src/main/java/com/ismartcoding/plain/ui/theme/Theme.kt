package com.ismartcoding.plain.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.preferences.LocalAmoledDarkTheme

@Composable
fun AppTheme(
    useDarkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) plainDarkColorScheme() else plainLightColorScheme(),
        typography = SystemTypography.applyTextDirection(),
        shapes = Shapes,
        content = content,
    )
}

@Composable
private fun plainDarkColorScheme(): ColorScheme {
    val amoled = LocalAmoledDarkTheme.current
    val bg = if (amoled) Color(0xFF000000) else Color(0xFF1C1C1E)
    val surface = if (amoled) Color(0xFF000000) else Color(0xFF2C2C2E)
    val surfaceVariant = if (amoled) Color(0xFF1C1C1E) else Color(0xFF2C2C2E)

    return darkColorScheme(
        primary = Color(0xFF0A84FF),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF003D99),
        onPrimaryContainer = Color(0xFFCCE4FF),
        inversePrimary = Color(0xFF007AFF),
        secondary = Color(0xFF0A84FF),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF003380),
        onSecondaryContainer = Color(0xFFCCDFFF),
        tertiary = Color(0xFFCCC2DC),
        onTertiary = Color(0xFF332D41),
        tertiaryContainer = Color(0xFF4A4458),
        onTertiaryContainer = Color(0xFFEADDFF),
        background = bg,
        onBackground = Color(0xFFFFFFFF),
        surface = surface,
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = Color(0xFF8D8D93),
        surfaceTint = Color(0xFF0A84FF).copy(alpha = 0.08f),
        inverseSurface = Color(0xFFF2F2F7),
        inverseOnSurface = Color(0xFF000000),
        outline = Color(0xFF38383A),
        outlineVariant = Color(0xFF48484A),
        surfaceBright = Color(0xFF2C2C2E),
        surfaceDim = Color(0xFF000000),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = if (amoled) Color(0xFF000000) else Color(0xFF1C1C1E),
        surfaceContainerHigh = Color(0xFF2C2C2E),
        surfaceContainerHighest = Color(0xFF3A3A3C),
    )
}

private fun plainLightColorScheme(): ColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E9FF),
    onPrimaryContainer = Color(0xFF001E3C),
    inversePrimary = Color(0xFF4DA3FF),
    secondary = Color(0xFF007AFF),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5F0FF),
    onSecondaryContainer = Color(0xFF001B47),
    tertiary = Color(0xFF625B71),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8DEF8),
    onTertiaryContainer = Color(0xFF1D192B),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF8E8E93),
    surfaceTint = Color(0xFF007AFF).copy(alpha = 0.05f),
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color(0xFFFFFFFF),
    outline = Color(0xFFC6C6C8),
    outlineVariant = Color(0xFFE5E5EA),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE5E5EA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F9FB),
    surfaceContainer = Color(0xFFEEF1F9),
    surfaceContainerHigh = Color(0xFFEAEAF0),
    surfaceContainerHighest = Color(0xFFE5E5EA),
)
