package com.ismartcoding.plain.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable

@Composable
fun PSwitch(
    activated: Boolean,
    enabled: Boolean = true,
    onClick: ((Boolean) -> Unit)? = null,
) {
    Switch(
        checked = activated,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.primary,
            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            disabledCheckedThumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        ),
        onCheckedChange = {
            onClick?.invoke(it)
        })
}
