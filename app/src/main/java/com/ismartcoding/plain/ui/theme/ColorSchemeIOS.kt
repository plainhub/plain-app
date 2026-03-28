package com.ismartcoding.plain.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.preferences.LocalDarkTheme

// iOS system green
val ColorScheme.green: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFF30D158) else Color(0xFF34C759)

// iOS system gray
val ColorScheme.grey: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFF636366) else Color(0xFF8E8E93)

// iOS system red
val ColorScheme.red: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFFFF453A) else Color(0xFFFF3B30)

// iOS system blue (delegates to Material primary)
val ColorScheme.blue: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary

// iOS system yellow
val ColorScheme.yellow: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFFFFD60A) else Color(0xFFFFCC00)

// iOS system orange
val ColorScheme.orange: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFFFF9F0A) else Color(0xFFFF9500)

// iOS system indigo
val ColorScheme.indigo: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFF5E5CE6) else Color(0xFF5856D6)

// iOS system teal
val ColorScheme.teal: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFF40CBE0) else Color(0xFF5AC8FA)

// iOS system pink
val ColorScheme.pink: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFFFF375F) else Color(0xFFFF2D55)

// iOS system purple
val ColorScheme.purple: Color
    @Composable @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) Color(0xFFBF5AF2) else Color(0xFFAF52DE)
