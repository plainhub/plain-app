package com.ismartcoding.plain.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun SwitchDefaults.plainColors(): SwitchColors {
    val scheme = MaterialTheme.colorScheme
    val dark = LocalDarkTheme.current
    val disabledContainer = when {
        !dark -> Color(0xFFE0E3EA)
        LocalAmoledDarkTheme.current -> Color(0xFF1E242D)
        else -> Color(0xFF2E323C)
    }
    val disabledContent = when {
        !dark -> Color(0xFF8A909C)
        LocalAmoledDarkTheme.current -> Color(0xFF6D7483)
        else -> Color(0xFF747B8A)
    }
    val uncheckedTrack = if (dark) Color(0xFF8993A8) else Color(0xFF707A8E)
    return colors(
        checkedTrackColor = scheme.primary,
        checkedThumbColor = scheme.onPrimary,
        checkedBorderColor = scheme.primary,
        uncheckedTrackColor = uncheckedTrack,
        uncheckedThumbColor = if (dark) Color(0xFF20232B) else Color(0xFFFAFAFC),
        uncheckedBorderColor = uncheckedTrack,
        disabledCheckedTrackColor = disabledContainer,
        disabledCheckedThumbColor = disabledContent,
        disabledCheckedBorderColor = disabledContainer,
        disabledUncheckedTrackColor = disabledContainer,
        disabledUncheckedThumbColor = disabledContent,
        disabledUncheckedBorderColor = disabledContainer,
    )
}
