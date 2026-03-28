package com.ismartcoding.plain.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.preferences.LocalDarkTheme

// --- Background & card ---

val ColorScheme.backgroundNormal: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFF1C1B1F) else Color(0xFFFFFBFE)

val ColorScheme.cardBackgroundNormal: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFF2C2C2E) else Color(0xFFf5f2ff)

val ColorScheme.cardBackgroundActive: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)

val ColorScheme.circleBackground: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFF2C2C2E) else Color(0xFFFFFFFF)

// --- Text & label ---

val ColorScheme.secondaryTextColor: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFF8D8D93) else Color(0xFF8E8E93)

// --- Audio waveform ---

val ColorScheme.waveActiveColor: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary

val ColorScheme.waveInactiveColor: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFF48484A) else Color(0xFFE5E5EA)

val ColorScheme.waveThumbColor: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary

val ColorScheme.badgeBorderColor: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)

@Composable
fun ColorScheme.lightMask(): Color = Color.White.copy(alpha = 0.4f)

@Composable
fun ColorScheme.darkMask(alpha: Float = 0.4f): Color = Color.Black.copy(alpha = alpha)
