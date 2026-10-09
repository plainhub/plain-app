package com.ismartcoding.plain.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal

object PSegmentedButtonsDefaults {
    @Composable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
        selectedContainerColor: Color = MaterialTheme.colorScheme.cardBackgroundNormal,
        contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedContentColor: Color = MaterialTheme.colorScheme.onSurface,
    ) = PSegmentedButtonsColors(containerColor, selectedContainerColor, contentColor, selectedContentColor)
}
