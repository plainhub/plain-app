package com.ismartcoding.plain.ui.components

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

enum class ButtonType {
    PRIMARY, DANGER, TERTIARY, TONAL;

    @Composable
    fun containerColor() = when (this) {
        PRIMARY -> MaterialTheme.colorScheme.primary
        TONAL -> MaterialTheme.colorScheme.primaryContainer
        DANGER -> MaterialTheme.colorScheme.error
        TERTIARY -> MaterialTheme.colorScheme.tertiary
    }

    @Composable
    fun contentColor() = when (this) {
        PRIMARY -> MaterialTheme.colorScheme.onPrimary
        TONAL -> MaterialTheme.colorScheme.onPrimaryContainer
        DANGER -> MaterialTheme.colorScheme.onError
        TERTIARY -> MaterialTheme.colorScheme.onTertiary
    }

    @Composable
    fun getColors() = ButtonDefaults.buttonColors(containerColor = containerColor(), contentColor = contentColor())
}
