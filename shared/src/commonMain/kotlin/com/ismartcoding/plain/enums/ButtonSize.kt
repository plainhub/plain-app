package com.ismartcoding.plain.enums

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ButtonSize(val height: Dp, val cornerRadius: Dp) {
    SMALL(32.dp, 16.dp),
    MEDIUM(40.dp, 20.dp),
    LARGE(48.dp, 24.dp),
    EXTRA_LARGE(56.dp, 28.dp);

    @Composable
    fun textStyle() = when (this) {
        SMALL -> MaterialTheme.typography.labelLarge
        MEDIUM -> MaterialTheme.typography.labelLarge
        LARGE -> MaterialTheme.typography.titleSmall
        EXTRA_LARGE -> MaterialTheme.typography.titleMedium
    }

    fun fontWeight() = when (this) {
        SMALL -> FontWeight.Medium
        MEDIUM -> FontWeight.SemiBold
        LARGE -> FontWeight.SemiBold
        EXTRA_LARGE -> FontWeight.Bold
    }

    @Composable
    fun elevation() = when (this) {
        SMALL -> ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )

        MEDIUM -> ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 1.dp
        )

        LARGE -> ButtonDefaults.buttonElevation(
            defaultElevation = 1.dp,
            pressedElevation = 0.dp
        )

        EXTRA_LARGE -> ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 1.dp
        )
    }
}
