package com.ismartcoding.plain.enums

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ButtonSize(val height: Dp) {
    SMALL(32.dp),
    MEDIUM(40.dp),
    LARGE(48.dp);

    @Composable
    fun textStyle() = when (this) {
        SMALL -> MaterialTheme.typography.labelLarge
        MEDIUM -> MaterialTheme.typography.labelLarge
        LARGE -> MaterialTheme.typography.titleMedium
    }

    fun fontWeight() = FontWeight.SemiBold

    fun getPaddingValues(): PaddingValues {
        return PaddingValues(horizontal = if (this == ButtonSize.SMALL) 12.dp else 16.dp)
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
    }
}
