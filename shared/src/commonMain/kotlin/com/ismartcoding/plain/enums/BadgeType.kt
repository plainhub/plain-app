package com.ismartcoding.plain.enums

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.ui.theme.greenDot


enum class BadgeType {
    NEUTRAL,
    ON,
    WARN,
    DANGER,
    INFO,
    SUCCESS;

    @Composable
    @ReadOnlyComposable
    fun container(): Color = when (this) {
        NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
        ON -> MaterialTheme.colorScheme.secondaryContainer
        WARN -> MaterialTheme.colorScheme.tertiaryContainer
        DANGER -> MaterialTheme.colorScheme.errorContainer
        INFO -> MaterialTheme.colorScheme.primaryContainer
        SUCCESS -> MaterialTheme.colorScheme.greenDot
    }

    @Composable
    @ReadOnlyComposable
    fun onContainer(): Color = when (this) {
        NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        ON -> MaterialTheme.colorScheme.onSecondaryContainer
        WARN -> MaterialTheme.colorScheme.onTertiaryContainer
        DANGER -> MaterialTheme.colorScheme.onErrorContainer
        INFO -> MaterialTheme.colorScheme.onPrimaryContainer
        SUCCESS -> Color.White
    }
}