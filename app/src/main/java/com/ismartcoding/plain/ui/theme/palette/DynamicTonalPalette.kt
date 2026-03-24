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
        // iOS systemBlue light
        primary = PlainColors.Light.blue,           // #007AFF
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD1E9FF),
        onPrimaryContainer = Color(0xFF001E3C),
        inversePrimary = Color(0xFF4DA3FF),
        // iOS systemBlue light (secondary)
        secondary = Color(0xFF007AFF),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE5F0FF),
        onSecondaryContainer = Color(0xFF001B47),
        tertiary = palettes tertiary 40,
        onTertiary = palettes tertiary 100,
        tertiaryContainer = palettes tertiary 90,
        onTertiaryContainer = palettes tertiary 10,
        // iOS systemGroupedBackground light = #EEF1F9
        background = Color(0xFFEEF1F9),
        onBackground = Color(0xFF000000),           // iOS label
        // iOS secondarySystemGroupedBackground = #FFFFFF (cards)
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF000000),              // iOS label
        surfaceVariant = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFF8E8E93),       // iOS secondaryLabel
        surfaceTint = Color(0xFF007AFF).copy(alpha = 0.05f),
        inverseSurface = Color(0xFF1C1C1E),
        inverseOnSurface = Color(0xFFFFFFFF),
        // iOS separator light = #C6C6C8
        outline = Color(0xFFC6C6C8),
        // iOS opaqueSeparator light = #E5E5EA
        outlineVariant = Color(0xFFE5E5EA),
        surfaceBright = Color(0xFFFFFFFF),
        surfaceDim = Color(0xFFE5E5EA),
        // surfaceContainer* used for nested backgrounds
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF9F9FB),
        surfaceContainer = Color(0xFFEEF1F9),       // systemGroupedBackground
        surfaceContainerHigh = Color(0xFFEAEAF0),
        surfaceContainerHighest = Color(0xFFE5E5EA),
    )
}

@Composable
fun dynamicDarkColorScheme(): ColorScheme {
    val palettes = LocalTonalPalettes.current
    val amoledDarkTheme = LocalAmoledDarkTheme.current

    // iOS dark systemGroupedBackground = #000000 for amoled, #1C1C1E for non-amoled
    val backgroundColor = if (amoledDarkTheme) Color(0xFF000000) else Color(0xFF1C1C1E)
    // iOS dark secondarySystemGroupedBackground = #2C2C2E (cards) when bg is #1C1C1E
    val surfaceColor = if (amoledDarkTheme) Color(0xFF000000) else Color(0xFF2C2C2E)
    // iOS dark tertiarySystemGroupedBackground = #2C2C2E
    val surfaceVariantColor = if (amoledDarkTheme) Color(0xFF1C1C1E) else Color(0xFF2C2C2E)
    val surfaceDimColor = if (amoledDarkTheme) Color(0xFF000000) else Color(0xFF000000)
    val surfaceContainerLowestColor = if (amoledDarkTheme) Color(0xFF000000) else Color(0xFF000000)
    val surfaceContainerLowColor = if (amoledDarkTheme) Color(0xFF000000) else Color(0xFF1C1C1E)

    return darkColorScheme(
        // iOS systemBlue dark = #0A84FF
        primary = PlainColors.Dark.blue,            // #0A84FF
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF003D99),
        onPrimaryContainer = Color(0xFFCCE4FF),
        inversePrimary = Color(0xFF007AFF),
        // iOS systemBlue dark (secondary)
        secondary = Color(0xFF0A84FF),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF003380),
        onSecondaryContainer = Color(0xFFCCDFFF),
        tertiary = palettes tertiary 80,
        onTertiary = palettes tertiary 20,
        tertiaryContainer = palettes tertiary 30,
        onTertiaryContainer = palettes tertiary 90,
        // iOS dark systemGroupedBackground = #000000
        background = backgroundColor,
        onBackground = Color(0xFFFFFFFF),           // iOS dark label
        // iOS dark secondarySystemGroupedBackground = #1C1C1E
        surface = surfaceColor,
        onSurface = Color(0xFFFFFFFF),              // iOS dark label
        surfaceVariant = surfaceVariantColor,
        onSurfaceVariant = Color(0xFF8D8D93),       // iOS dark secondaryLabel
        surfaceTint = Color(0xFF0A84FF).copy(alpha = 0.08f),
        inverseSurface = Color(0xFFF2F2F7),
        inverseOnSurface = Color(0xFF000000),
        // iOS dark separator = #38383A
        outline = Color(0xFF38383A),
        // iOS dark opaqueSeparator = #48484A
        outlineVariant = Color(0xFF48484A),
        // iOS dark tertiarySystemGroupedBackground = #2C2C2E
        surfaceBright = Color(0xFF2C2C2E),
        surfaceDim = surfaceDimColor,
        surfaceContainerLowest = surfaceContainerLowestColor,
        surfaceContainerLow = surfaceContainerLowColor,
        // iOS dark quaternarySystemFill = #3A3A3C
        surfaceContainerHigh = Color(0xFF2C2C2E),
        surfaceContainerHighest = Color(0xFF3A3A3C),
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
