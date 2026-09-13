package com.ismartcoding.plain.ui.base
import com.ismartcoding.plain.preferences.*

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.preferences.LocalDarkTheme
import com.ismartcoding.plain.ui.theme.filledButtonContent

@Composable
fun PSwitch(
    activated: Boolean,
    enabled: Boolean = true,
    onClick: ((Boolean) -> Unit)? = null,
) {
    val isDark = DarkTheme.isDarkTheme(LocalDarkTheme.current)

    val switchBlue = if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)
    val iosLightTrackGray = Color(0xFFE9E9EA)
    val iosDarkTrackGray = Color(0xFF39393D)
    val iosTrackGray = if (isDark) iosDarkTrackGray else iosLightTrackGray
    // Pure white thumb glares on dark tracks and backgrounds (user, 2026-09-13);
    // filledButtonContent is the app's soft white in dark mode, white in light.
    val thumbColor = MaterialTheme.colorScheme.filledButtonContent

    val disabledThumbColor = thumbColor
    val disabledCheckedTrack = switchBlue.copy(alpha = 0.4f)
    val disabledUncheckedTrack = iosTrackGray.copy(alpha = if (isDark) 0.3f else 0.5f)

    Switch(
        checked = activated,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = thumbColor,
            checkedTrackColor = switchBlue,
            uncheckedThumbColor = thumbColor,
            uncheckedTrackColor = iosTrackGray,

            // Disabled states
            disabledCheckedThumbColor = disabledThumbColor,
            disabledCheckedTrackColor = disabledCheckedTrack,
            disabledUncheckedThumbColor = disabledThumbColor,
            disabledUncheckedTrackColor = disabledUncheckedTrack,
        ),
        onCheckedChange = {
            onClick?.invoke(it)
        })
}
