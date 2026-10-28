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
    get() = greenText

val ColorScheme.onGreen: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF153A24) else Color.White

val ColorScheme.grey: Color
    @Composable @ReadOnlyComposable
    get() = when {
        !LocalDarkTheme.current -> Color(0xFF8A909C)
        LocalAmoledDarkTheme.current -> Color(0xFF6D7483)
        else -> Color(0xFF747B8A)
    }

val ColorScheme.yellow: Color
    @Composable @ReadOnlyComposable
    get() = warning

val ColorScheme.orange: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFFC9A685) else Color(0xFF92542F)

val LocalFloatingHostColor: ProvidableCompositionLocal<Color?> = staticCompositionLocalOf { null }

val ColorScheme.filledButtonContent: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) this.onSurface else Color(0xFFFFFFFF)

val ColorScheme.backgroundNormal: Color
    get() = background

val ColorScheme.cardBackgroundNormal: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalFloatingHostColor.current == null) surfaceContainer else surfaceContainerHigh

val ColorScheme.dialogSheetBackground: Color
    get() = surfaceContainerLow

val ColorScheme.cardBackgroundActive: Color
    get() = primaryContainer

val ColorScheme.circleBackground: Color
    @Composable @ReadOnlyComposable
    get() = cardBackgroundNormal

val ColorScheme.warning: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFFC8B17A) else Color(0xFF805600)

val ColorScheme.warningContainer: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF443B26) else Color(0xFFF1E4C8)

val ColorScheme.onWarningContainer: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFFD7C69B) else Color(0xFF614711)

val ColorScheme.greenDot: Color
    @Composable @ReadOnlyComposable
    get() = greenText

val ColorScheme.greenText: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF9EBDA7) else Color(0xFF356B4D)

val ColorScheme.greenPill: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF263D30) else Color(0xFFDEECE2)

val ColorScheme.primaryPill: Color
    get() = primaryContainer

val ColorScheme.waveInactiveColor: Color
    get() = outlineVariant

val ColorScheme.badgeBorderColor: Color
    get() = surface

val ColorScheme.searchHighlight: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) Color(0xFF564A2D) else Color(0xFFF2E3B4)

@Composable
fun ColorScheme.lightMask(): Color = Color.White.copy(alpha = 0.4f)

@Composable
fun ColorScheme.darkMask(alpha: Float = 0.4f): Color = Color.Black.copy(alpha = alpha)
