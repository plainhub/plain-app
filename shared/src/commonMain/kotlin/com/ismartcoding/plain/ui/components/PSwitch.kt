package com.ismartcoding.plain.ui.components

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.ui.theme.LocalDarkTheme

@Composable
internal fun PSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors().copy(
            uncheckedThumbColor = MaterialTheme.colorScheme.uncheckedSwitchThumb(LocalDarkTheme.current),
        ),
    )
}

internal fun ColorScheme.uncheckedSwitchThumb(darkTheme: Boolean): Color = if (darkTheme) onSurfaceVariant else Color.Unspecified
