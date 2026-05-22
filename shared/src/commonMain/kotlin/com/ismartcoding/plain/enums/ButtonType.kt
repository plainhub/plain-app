package com.ismartcoding.plain.enums

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.red


enum class ButtonType {
    PRIMARY,
    DANGER,
    TERTIARY;

    @Composable
    fun getColors(): ButtonColors {
        return when(this) {
            PRIMARY -> {
                ButtonDefaults.buttonColors()
            }

            DANGER -> {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.red,
                    contentColor = Color.White,
                )
            }

            TERTIARY -> {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                )
            }
        }
    }
}