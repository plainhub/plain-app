package com.ismartcoding.plain.ui.base

import androidx.compose.material3.Switch
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
        onCheckedChange = onClick,
    )
}
