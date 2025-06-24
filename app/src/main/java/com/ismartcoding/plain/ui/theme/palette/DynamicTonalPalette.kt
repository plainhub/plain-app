package com.ismartcoding.plain.ui.theme.palette

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.preferences.LocalAmoledDarkTheme
import com.ismartcoding.plain.preferences.LocalDarkTheme
import com.ismartcoding.plain.ui.theme.PlainColors

@Composable
fun dynamicLightColorScheme(): ColorScheme {
    val palettes = LocalTonalPalettes.current
    return lightColorScheme(
        primary = PlainColors.Light.blue,
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD2E4FF),
        onPrimaryContainer = Color(0xFF001E3C),
        inversePrimary = palettes primary 80,
        secondary = Color(0xFF42A5F5),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE3F2FD),
        onSecondaryContainer = Color(0xFF0D47A1),
        tertiary = palettes tertiary 40,
        onTertiary = palettes tertiary 100,
        tertiaryContainer = palettes tertiary 90,
        onTertiaryContainer = palettes tertiary 10,
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFF202124),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF202124),
        surfaceVariant = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFF5F6368),
        surfaceTint = Color(0xFF2196F3).copy(alpha = 0.1f),
        inverseSurface = Color(0xFF202124),
        inverseOnSurface = Color(0xFFF8F9FA),
        outline = Color(0xFFDADCE0),
        outlineVariant = Color(0xFFEEEEEE),
        surfaceBright = Color(0xFFFFFFFF),
        surfaceDim = Color(0xFFF1F3F4),
        surfaceContainerLowest = Color(0xFFEFF6FF),
        surfaceContainerLow = Color(0xFFECF2FF),
        surfaceContainer = Color(0xFFE3EFFF),
        surfaceContainerHigh = Color(0xFFDCECFF),
        surfaceContainerHighest = Color(0xFFD1E8FF),
    )
}

@Composable
fun dynamicDarkColorScheme(): ColorScheme {
    val palettes = LocalTonalPalettes.current
    val amoledDarkTheme = LocalAmoledDarkTheme.current
    
    // Use true black (#000000) for background and surfaces when amoledDarkTheme is enabled
    val backgroundColor = if (amoledDarkTheme) Color(0xFF000000) else Color(0xFF121212)
    val surfaceColor = if (amoledDarkTheme) Color(0xFF000000) else Color(0xFF121212)
    val surfaceVariantColor = if (amoledDarkTheme) Color(0xFF000000) else Color(0xFF202124)
    val surfaceDimColor = if (amoledDarkTheme) Color(0xFF000000) else Color(0xFF101010)
    val surfaceContainerLowestColor = if (amoledDarkTheme) Color(0xFF000000) else Color(0xFF0A0A0A)
    val surfaceContainerLowColor = if (amoledDarkTheme) Color(0xFF000000) else Color(0xFF121212)

    return darkColorScheme(
        primary = PlainColors.Dark.blue,
        onPrimary = Color(0xFFE8EAED),
        primaryContainer = Color(0xFF004D8F),
        onPrimaryContainer = Color(0xFFD6E3FF),
        inversePrimary = palettes primary 40,
        secondary = Color(0xFF82B1FF),
        onSecondary = Color(0xFF002171),
        secondaryContainer = Color(0xFF1A237E),
        onSecondaryContainer = Color(0xFFE6EEFF),
        tertiary = palettes tertiary 80,
        onTertiary = palettes tertiary 20,
        tertiaryContainer = palettes tertiary 30,
        onTertiaryContainer = palettes tertiary 90,
        background = backgroundColor,
        onBackground = Color(0xFFE8EAED),
        surface = surfaceColor,
        onSurface = Color(0xFFE8EAED),
        surfaceVariant = surfaceVariantColor,
        onSurfaceVariant = Color(0xFFBDC1C6),
        surfaceTint = Color(0xFF64B5F6).copy(alpha = 0.2f),
        inverseSurface = Color(0xFFF8F9FA),
        inverseOnSurface = Color(0xFF202124),
        outline = Color(0xFF5F6368),
        outlineVariant = Color(0xFF3C4043),
        surfaceBright = Color(0xFF3C4043),
        surfaceDim = surfaceDimColor,
        surfaceContainerLowest = surfaceContainerLowestColor,
        surfaceContainerLow = surfaceContainerLowColor,
        surfaceContainerHigh = Color(0xFF202124),
        surfaceContainerHighest = Color(0xFF292A2D),
    )
}

@Composable
infix fun Color.onDark(darkColor: Color): Color = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) darkColor else this

@Stable
@Composable
@ReadOnlyComposable
infix fun Color.alwaysLight(isAlways: Boolean): Color {
    val colorScheme = MaterialTheme.colorScheme
    return if (isAlways && DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        when (this) {
            colorScheme.primary -> colorScheme.onPrimary
            colorScheme.secondary -> colorScheme.onSecondary
            colorScheme.tertiary -> colorScheme.onTertiary
            colorScheme.background -> colorScheme.onBackground
            colorScheme.error -> colorScheme.onError
            colorScheme.surface -> colorScheme.onSurface
            colorScheme.surfaceVariant -> colorScheme.onSurfaceVariant
            colorScheme.primaryContainer -> colorScheme.onPrimaryContainer
            colorScheme.secondaryContainer -> colorScheme.onSecondaryContainer
            colorScheme.tertiaryContainer -> colorScheme.onTertiaryContainer
            colorScheme.errorContainer -> colorScheme.onErrorContainer
            colorScheme.inverseSurface -> colorScheme.inverseOnSurface

            colorScheme.onPrimary -> colorScheme.primary
            colorScheme.onSecondary -> colorScheme.secondary
            colorScheme.onTertiary -> colorScheme.tertiary
            colorScheme.onBackground -> colorScheme.background
            colorScheme.onError -> colorScheme.error
            colorScheme.onSurface -> colorScheme.surface
            colorScheme.onSurfaceVariant -> colorScheme.surfaceVariant
            colorScheme.onPrimaryContainer -> colorScheme.primaryContainer
            colorScheme.onSecondaryContainer -> colorScheme.secondaryContainer
            colorScheme.onTertiaryContainer -> colorScheme.tertiaryContainer
            colorScheme.onErrorContainer -> colorScheme.errorContainer
            colorScheme.inverseOnSurface -> colorScheme.inverseSurface

            else -> Color.Unspecified
        }
    } else {
        this
    }
}

fun String.checkColorHex(): String? {
    var s = this.trim()
    if (s.length > 6) {
        s = s.substring(s.length - 6)
    }
    return "[0-9a-fA-F]{6}".toRegex().find(s)?.value
}

@Stable
fun String.safeHexToColor(): Color =
    try {
        Color(java.lang.Long.parseLong(this, 16))
    } catch (e: Exception) {
        Color.Transparent
    }
