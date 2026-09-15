package com.ismartcoding.plain.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.preferences.LocalAmoledDarkTheme
import com.ismartcoding.plain.preferences.LocalDarkTheme

@Composable
fun AppTheme(useDarkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) plainDarkColorScheme() else plainLightColorScheme(),
        typography = SystemTypography.applyTextDirection(),
        shapes = Shapes,
        content = content,
    )
}

/** Soft near-white for dark-theme text: #E5E5EA glared at large sizes (user, 2026-09-13). */
private val DarkSoftOnSurface = Color(0xFFD1D1D6)

/** Brand indigo (light primary): single source for the scheme and non-Compose surfaces (notification accent). */
internal val BrandPrimary = Color(0xFF4F5F9E)

@Composable
private fun plainDarkColorScheme(): ColorScheme {
    val amoled = LocalAmoledDarkTheme.current
    val bg = if (amoled) Color(0xFF000000) else Color(0xFF1C1C1E)
    val surface = if (amoled) Color(0xFF000000) else Color(0xFF2C2C2E)
    val surfaceVariant = if (amoled) Color(0xFF1C1C1E) else Color(0xFF2C2C2E)
    return darkColorScheme(
        // Primary family per user-provided soft indigo pair (2026-09-14,
        // replacing the iOS blue): pastel indigo fill with dark navy content.
        primary = Color(0xFFB8C4FF), onPrimary = Color(0xFF1F2E61),
        primaryContainer = Color(0xFF374777), onPrimaryContainer = Color(0xFFDDE2F9),
        inversePrimary = BrandPrimary,
        secondary = Color(0xFFB8C4FF), onSecondary = Color(0xFF1F2E61),
        // Dark container synced from plain-desktop: muted indigo replaces the
        // glaring saturated blue pill (#003380) and ice-blue text (#CCDFFF).
        // Light keeps the pale pair — selected rows there carry onSurface
        // black text, which would fail on the mid-tone desktop container.
        secondaryContainer = Color(0xFF363CA2), onSecondaryContainer = Color(0xFFADB2FF),
        tertiary = Color(0xFFCCC2DC), onTertiary = Color(0xFF332D41),
        tertiaryContainer = Color(0xFF4A4458), onTertiaryContainer = Color(0xFFEADDFF),
        // Error family synced from plain-desktop (M3 baseline, user 2026-09-14
        // asked for a less bright red): pale salmon in dark, deep muted red in
        // light. Content on the pale fill uses the dark onError.
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        background = bg, onBackground = DarkSoftOnSurface,
        surface = surface, onSurface = DarkSoftOnSurface,
        surfaceVariant = surfaceVariant, onSurfaceVariant = Color(0xFF8D8D93),
        surfaceTint = Color(0xFFB8C4FF).copy(alpha = 0.08f),
        inverseSurface = Color(0xFFF2F2F7), inverseOnSurface = Color(0xFF000000),
        outline = Color(0xFF38383A), outlineVariant = Color(0xFF48484A),
        scrim = Color(0xFF000000),
        // Surface container ramp synced from plain-desktop _base.scss (2026-09-14).
        // Never forced to black in amoled: containers must stay lifted above
        // the pure black background or cards blend into it.
        surfaceBright = Color(0xFF383844),
        surfaceDim = if (amoled) Color(0xFF000000) else Color(0xFF12121D),
        surfaceContainer = Color(0xFF1E1F2A),
        surfaceContainerLowest = Color(0xFF0D0D18),
        surfaceContainerLow = Color(0xFF2C2C2E),
        surfaceContainerHigh = Color(0xFF292935),
        surfaceContainerHighest = Color(0xFF333440),
    )
}

private fun plainLightColorScheme(): ColorScheme = lightColorScheme(
    primary = BrandPrimary, onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE2F9), onPrimaryContainer = Color(0xFF111A3A),
    inversePrimary = Color(0xFFB8C4FF),
    secondary = BrandPrimary, onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5F0FF), onSecondaryContainer = Color(0xFF001B47),
    tertiary = Color(0xFF625B71), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8DEF8), onTertiaryContainer = Color(0xFF1D192B),
    // M3 baseline error, synced with plain-desktop (2026-09-14).
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFE), onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFFFFFFF), onSurfaceVariant = Color(0xFF636366),
    surfaceTint = BrandPrimary.copy(alpha = 0.05f),
    inverseSurface = Color(0xFF1C1C1E), inverseOnSurface = Color(0xFFFFFFFF),
    outline = Color(0xFFC6C6C8), outlineVariant = Color(0xFFE5E5EA),
    scrim = Color(0xFF000000),
    // Surface container ramp synced from plain-desktop _base.scss (2026-09-14).
    surfaceBright = Color(0xFFFBF8FF), surfaceDim = Color(0xFFDAD8E8),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF5F2FF),
    surfaceContainer = Color(0xFFEEECFC), surfaceContainerHigh = Color(0xFFE8E6F6),
    surfaceContainerHighest = Color(0xFFE3E1F1),
)

val ColorScheme.green: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFF30D158) else Color(0xFF34C759)

val ColorScheme.grey: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFF636366) else Color(0xFF8E8E93)

val ColorScheme.yellow: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFFFFD60A) else Color(0xFFFFCC00)

val ColorScheme.orange: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFFFF9F0A) else Color(0xFFFF9500)

// -------- App semantic colors --------

// Soft content for filled surfaces that stay dark in dark mode (danger red,
// unchecked switch track): soft white there, white in light. Content on the
// pastel primary fill uses onPrimary (dark navy) instead.
val ColorScheme.filledButtonContent: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) DarkSoftOnSurface else Color(0xFFFFFFFF)

val ColorScheme.backgroundNormal: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFF1C1B1F) else Color(0xFFFFFBFE)

val ColorScheme.cardBackgroundNormal: Color
    @Composable @ReadOnlyComposable
    get() = this.surfaceContainerLow

// Selected/playing cards, same treatment as plain-desktop .selectable-card.selected.
val ColorScheme.cardBackgroundActive: Color
    @Composable @ReadOnlyComposable
    get() = this.surfaceContainerHighest

val ColorScheme.circleBackground: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFF2C2C2E) else Color(0xFFFFFFFF)

val ColorScheme.greenDot: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFF66BB6A) else Color(0xFF4CAF50)

val ColorScheme.greenText: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFFA5D6A7) else Color(0xFF2E7D32)

val ColorScheme.greenPill: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0x4D1B5E20) else Color(0xFFE8F5E9)

val ColorScheme.primaryPill: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0x33B8C4FF)
    } else {
        Color(0xFFDDE2F9)
    }

val ColorScheme.waveInactiveColor: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFF48484A) else Color(0xFFE5E5EA)

val ColorScheme.badgeBorderColor: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)


@Composable
fun ColorScheme.lightMask(): Color = Color.White.copy(alpha = 0.4f)

@Composable
fun ColorScheme.darkMask(alpha: Float = 0.4f): Color = Color.Black.copy(alpha = alpha)
