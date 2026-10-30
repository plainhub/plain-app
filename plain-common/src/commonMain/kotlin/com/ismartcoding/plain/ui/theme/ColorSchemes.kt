package com.ismartcoding.plain.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val LightColors = lightColorScheme(
    primary = BrandPrimary, onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE2F9), onPrimaryContainer = Color(0xFF111A3A),
    inversePrimary = Color(0xFFB8C4FF),
    secondary = BrandPrimary, onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5F0FF), onSecondaryContainer = Color(0xFF001B47),
    tertiary = Color(0xFF625B71), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8DEF8), onTertiaryContainer = Color(0xFF1D192B),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFE), onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFFFFFFF), onSurfaceVariant = Color(0xFF636366),
    surfaceTint = BrandPrimary.copy(alpha = 0.05f),
    inverseSurface = Color(0xFF1C1C1E), inverseOnSurface = Color(0xFFFFFFFF),
    outline = Color(0xFFC6C6C8), outlineVariant = Color(0xFFE5E5EA),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFBF8FF), surfaceDim = Color(0xFFDAD8E8),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF5F2FF),
    surfaceContainer = Color(0xFFEEECFC), surfaceContainerHigh = Color(0xFFE8E6F6),
    surfaceContainerHighest = Color(0xFFE3E1F1),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFB8C4FF), onPrimary = Color(0xFF1F2E61),
    primaryContainer = Color(0xFF374777), onPrimaryContainer = Color(0xFFDDE2F9),
    inversePrimary = BrandPrimary,
    secondary = Color(0xFFB8C4FF), onSecondary = Color(0xFF1F2E61),
    secondaryContainer = Color(0xFF363CA2), onSecondaryContainer = Color(0xFFADB2FF),
    tertiary = Color(0xFFCCC2DC), onTertiary = Color(0xFF332D41),
    tertiaryContainer = Color(0xFF4A4458), onTertiaryContainer = Color(0xFFEADDFF),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1C1C1E), onBackground = Color(0xFFD1D1D6),
    surface = Color(0xFF2C2C2E), onSurface = Color(0xFFD1D1D6),
    surfaceVariant = Color(0xFF2C2C2E), onSurfaceVariant = Color(0xFF8D8D93),
    surfaceTint = Color(0xFFB8C4FF).copy(alpha = 0.08f),
    inverseSurface = Color(0xFFF2F2F7), inverseOnSurface = Color(0xFF000000),
    outline = Color(0xFF38383A), outlineVariant = Color(0xFF48484A),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF383844),
    surfaceDim = Color(0xFF12121D),
    surfaceContainer = Color(0xFF1E1F2A),
    surfaceContainerLowest = Color(0xFF0D0D18),
    surfaceContainerLow = Color(0xFF2C2C2E),
    surfaceContainerHigh = Color(0xFF292935),
    surfaceContainerHighest = Color(0xFF333440),
)

internal val AmoledColors = DarkColors.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF1C1C1E),
    surfaceDim = Color(0xFF000000),
)
