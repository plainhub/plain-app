package com.ismartcoding.plain.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.preference.LocalDarkTheme
import com.ismartcoding.plain.ui.theme.palette.onDark

@Composable
fun ColorScheme.cardContainer(): Color {
    return MaterialTheme.colorScheme.cardBackgroundNormal
}

@Composable
fun ColorScheme.bottomAppBarContainer(): Color {
    return MaterialTheme.colorScheme.cardBackgroundNormal
}

@Composable
fun ColorScheme.lightMask(): Color {
    return Color.White.copy(alpha = 0.4f)
}

@Composable
fun ColorScheme.darkMask(alpha: Float = 0.4f): Color {
    return Color.Black.copy(alpha = alpha)
}

val ColorScheme.green: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF4CAF50)
    } else {
        Color(0xFF4CAF50)
    }

val ColorScheme.grey: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF757575)
    } else {
        Color(0xFFBDBDBD)
    }


val ColorScheme.red: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        PlainColors.Dark.red
    } else {
        PlainColors.Light.red
    }


val ColorScheme.blue: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary

val ColorScheme.yellow: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFFFFEB3B)
    } else {
        Color(0xFFFFEB3B)
    }

val ColorScheme.orange: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFFFF9800)
    } else {
        Color(0xFFFF9800)
    }

val ColorScheme.navBarBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF121212)
    } else {
        Color.White
    }

val ColorScheme.navBarUnselectedColor: Color
    @Composable
    @ReadOnlyComposable
    get() = Color(0xFF78797A)

val ColorScheme.waveActiveColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF42A5F5)
    } else {
        Color(0xFF1976D2)
    }

val ColorScheme.waveInactiveColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF616161)
    } else {
        Color(0xFFE0E0E0)
    }

val ColorScheme.waveThumbColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF64B5F6)
    } else {
        Color(0xFF1976D2)
    }

val ColorScheme.surfaceBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF121212)
    } else {
        Color(0xFFF8F9FA)
    }

val ColorScheme.cardBackgroundNormal: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF1E293B)
    } else {
        Color(0xFFEFF6FF)
    }

val ColorScheme.cardBackgroundActive: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF334155)
    } else {
        Color(0xFFD1E9FF)
    }

val ColorScheme.circleBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF333333)
    } else {
        Color.White
    }

val ColorScheme.secondaryTextColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFFBDC1C6)
    } else {
        Color(0xFF5F6368)
    }

val ColorScheme.primaryTextColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFFE8EAED)
    } else {
        Color(0xFF202124)
    }

