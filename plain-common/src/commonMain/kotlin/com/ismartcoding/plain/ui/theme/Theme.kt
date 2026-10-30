package com.ismartcoding.plain.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun AppTheme(useDarkTheme: Boolean, amoledDarkTheme: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalDarkTheme provides useDarkTheme,
        LocalAmoledDarkTheme provides amoledDarkTheme,
    ) {
        MaterialTheme(
            colorScheme = when {
                !useDarkTheme -> LightColors
                amoledDarkTheme -> AmoledColors
                else -> DarkColors
            },
            typography = ContentTypography,
            shapes = Shapes,
            content = content,
        )
    }
}

val BrandPrimary = Color(0xFF4F5F9E)

val ColorScheme.green: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF81C784) else Color(0xFF43A047)

val ColorScheme.grey: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF636366) else Color(0xFF8E8E93)

val ColorScheme.yellow: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFFFFD60A) else Color(0xFFFFCC00)

val ColorScheme.orange: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFFFF9F0A) else Color(0xFFFF9500)

val LocalFloatingHostColor: ProvidableCompositionLocal<Color?> = staticCompositionLocalOf { null }

val ColorScheme.filledButtonContent: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) this.onSurface else Color(0xFFFFFFFF)

val ColorScheme.backgroundNormal: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF1C1B1F) else Color(0xFFFFFBFE)

val ColorScheme.cardBackgroundNormal: Color
    @Composable @ReadOnlyComposable
    get() {
        val base = this.surfaceContainerLow
        val host = LocalFloatingHostColor.current
        return if (host == base) this.surfaceContainerHighest else base
    }

val ColorScheme.dialogSheetBackground: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) this.surfaceContainerLow else this.surface

val ColorScheme.cardBackgroundActive: Color
    @Composable @ReadOnlyComposable
    get() {
        val base = this.surfaceContainerHighest
        val host = LocalFloatingHostColor.current
        return if (host == this.surfaceContainerLow) this.surfaceBright else base
    }

val ColorScheme.circleBackground: Color
    @Composable @ReadOnlyComposable
    get() {
        val base = if (LocalDarkTheme.current) Color(0xFF2C2C2E) else Color(0xFFFFFFFF)
        val host = LocalFloatingHostColor.current
        return if (host == base) this.surfaceBright else base
    }

val ColorScheme.greenDot: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF66BB6A) else Color(0xFF4CAF50)

val ColorScheme.greenText: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFFA5D6A7) else Color(0xFF2E7D32)

val ColorScheme.greenPill: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0x4D1B5E20) else Color(0xFFE8F5E9)

val ColorScheme.primaryPill: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) {
        Color(0x33B8C4FF)
    } else {
        Color(0xFFDDE2F9)
    }

val ColorScheme.waveInactiveColor: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF48484A) else Color(0xFFE5E5EA)

val ColorScheme.badgeBorderColor: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)

val ColorScheme.searchHighlight: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF5A512E) else Color(0xFFFFF1B8)

@Composable
fun ColorScheme.lightMask(): Color = Color.White.copy(alpha = 0.4f)

@Composable
fun ColorScheme.darkMask(alpha: Float = 0.4f): Color = Color.Black.copy(alpha = alpha)

val ColorScheme.onGreen: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF153A24) else Color.White

val ColorScheme.warning: Color
    @Composable @ReadOnlyComposable
    get() = orange

val ColorScheme.warningContainer: Color
    @Composable @ReadOnlyComposable
    get() = orange.copy(alpha = 0.15f)

val ColorScheme.onWarningContainer: Color
    @Composable @ReadOnlyComposable
    get() = onSurface
