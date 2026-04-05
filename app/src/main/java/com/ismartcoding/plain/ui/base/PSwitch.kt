package com.ismartcoding.plain.ui.base

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.preferences.LocalDarkTheme

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
    val iosThumbWhite = Color.White

    val disabledThumbColor = iosThumbWhite
    val disabledCheckedTrack = switchBlue.copy(alpha = 0.4f)
    val disabledUncheckedTrack = iosTrackGray.copy(alpha = if (isDark) 0.3f else 0.5f)

    Switch(
        checked = activated,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = iosThumbWhite,
            checkedTrackColor = switchBlue,
            uncheckedThumbColor = iosThumbWhite,
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
