package com.ismartcoding.plain.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.preference.LocalDarkTheme

@Composable
fun PSwitch(
    activated: Boolean,
    enabled: Boolean = true,
    onClick: ((Boolean) -> Unit)? = null,
) {
    val isDark = DarkTheme.isDarkTheme(LocalDarkTheme.current)

    // Exact iOS colors
    val iosGreen = Color(0xFF34C759)  // iOS green for ON state
    val iosLightTrackGray = Color(0xFFE9E9EA)  // iOS light mode gray track
    val iosDarkTrackGray = Color(0xFF39393D)   // iOS dark mode gray track
    val iosTrackGray = if (isDark) iosDarkTrackGray else iosLightTrackGray
    val iosThumbWhite = Color.White

    // Disabled colors
    val disabledThumbColor = iosThumbWhite
    val disabledCheckedTrack = iosGreen.copy(alpha = 0.4f)
    val disabledUncheckedTrack = iosTrackGray.copy(alpha = if (isDark) 0.3f else 0.5f)

    Switch(
        checked = activated,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            // Enabled states
            checkedThumbColor = iosThumbWhite,
            checkedTrackColor = iosGreen,
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
