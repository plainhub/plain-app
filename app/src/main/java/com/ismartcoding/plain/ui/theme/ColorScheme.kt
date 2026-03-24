package com.ismartcoding.plain.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.preferences.LocalDarkTheme

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

// iOS system green
val ColorScheme.green: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF30D158) // iOS dark systemGreen
    } else {
        Color(0xFF34C759) // iOS light systemGreen
    }

// iOS system gray
val ColorScheme.grey: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF636366) // iOS dark systemGray
    } else {
        Color(0xFF8E8E93) // iOS light systemGray
    }

// iOS system red
val ColorScheme.red: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        PlainColors.Dark.red  // #FF453A
    } else {
        PlainColors.Light.red // #FF3B30
    }

val ColorScheme.badgeBorderColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF1C1C1E) // iOS dark secondarySystemBackground
    } else {
        Color(0xFFFFFFFF)
    }

// iOS system blue
val ColorScheme.blue: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary

// iOS system yellow
val ColorScheme.yellow: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFFFFD60A) // iOS dark systemYellow
    } else {
        Color(0xFFFFCC00) // iOS light systemYellow
    }

// iOS system orange
val ColorScheme.orange: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFFFF9F0A) // iOS dark systemOrange
    } else {
        Color(0xFFFF9500) // iOS light systemOrange
    }

// iOS system indigo
val ColorScheme.indigo: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF5E5CE6) // iOS dark systemIndigo
    } else {
        Color(0xFF5856D6) // iOS light systemIndigo
    }

// iOS system teal
val ColorScheme.teal: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF40CBE0) // iOS dark systemTeal
    } else {
        Color(0xFF5AC8FA) // iOS light systemTeal
    }

// iOS system pink
val ColorScheme.pink: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFFFF375F) // iOS dark systemPink
    } else {
        Color(0xFFFF2D55) // iOS light systemPink
    }

// iOS system purple
val ColorScheme.purple: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFFBF5AF2) // iOS dark systemPurple
    } else {
        Color(0xFFAF52DE) // iOS light systemPurple
    }

// iOS grouped background (used as page background)
val ColorScheme.surfaceBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF1C1C1E) // iOS dark systemGroupedBackground (non-amoled)
    } else {
        Color(0xFFEEF1F9) // light systemGroupedBackground
    }

// iOS secondarySystemGroupedBackground — white card on gray bg (light) / elevated dark card (dark)
val ColorScheme.cardBackgroundNormal: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF2C2C2E) // iOS dark secondarySystemGroupedBackground
    } else {
        Color(0xFFFFFFFF) // iOS light secondarySystemGroupedBackground
    }

// iOS tertiarySystemGroupedBackground — slightly elevated card
val ColorScheme.cardBackgroundActive: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF2C2C2E) // iOS dark tertiarySystemGroupedBackground
    } else {
        Color(0xFFE5E5EA) // iOS light tertiarySystemGroupedBackground
    }

// iOS fill color for circular icon backgrounds
val ColorScheme.circleBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF2C2C2E) // iOS dark tertiarySystemBackground
    } else {
        Color(0xFFFFFFFF)
    }

// iOS secondary label
val ColorScheme.secondaryTextColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF8D8D93) // iOS dark secondaryLabel
    } else {
        Color(0xFF8E8E93) // iOS light secondaryLabel
    }

// iOS primary label
val ColorScheme.primaryTextColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFFFFFFFF) // iOS dark label
    } else {
        Color(0xFF000000) // iOS light label
    }

val ColorScheme.waveActiveColor: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary

val ColorScheme.waveInactiveColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF48484A) // iOS dark opaqueSeparator
    } else {
        Color(0xFFE5E5EA) // iOS light opaqueSeparator
    }

val ColorScheme.waveThumbColor: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary

// iOS navigationBar background
val ColorScheme.navBarBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF1C1C1E) // iOS dark secondarySystemGroupedBackground
    } else {
        Color(0xFFFFFFFF) // iOS light white
    }

val ColorScheme.navBarUnselectedColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
        Color(0xFF8D8D93) // iOS dark secondaryLabel
    } else {
        Color(0xFF8E8E93) // iOS light secondaryLabel
    }

