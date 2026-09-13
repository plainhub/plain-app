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

    val switchBlue = MaterialTheme.colorScheme.primary
    val iosLightTrackGray = Color(0xFFE9E9EA)
    val iosDarkTrackGray = Color(0xFF39393D)
    val iosTrackGray = if (isDark) iosDarkTrackGray else iosLightTrackGray
    // Checked track is the pastel primary in dark, so the thumb goes navy
    // (onPrimary); the unchecked dark track keeps the soft white thumb.
    val checkedThumbColor = MaterialTheme.colorScheme.onPrimary
    val uncheckedThumbColor = MaterialTheme.colorScheme.filledButtonContent

    val disabledCheckedTrack = switchBlue.copy(alpha = 0.4f)
    val disabledUncheckedTrack = iosTrackGray.copy(alpha = if (isDark) 0.3f else 0.5f)

    Switch(
        checked = activated,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = checkedThumbColor,
            checkedTrackColor = switchBlue,
            uncheckedThumbColor = uncheckedThumbColor,
            uncheckedTrackColor = iosTrackGray,

            // Disabled states
            disabledCheckedThumbColor = checkedThumbColor,
            disabledCheckedTrackColor = disabledCheckedTrack,
            disabledUncheckedThumbColor = uncheckedThumbColor,
            disabledUncheckedTrackColor = disabledUncheckedTrack,
        ),
        onCheckedChange = {
            onClick?.invoke(it)
        })
}
