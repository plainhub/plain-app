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
    SECONDARY,
    DANGER;

    @Composable
    fun getColors(): ButtonColors {
        return when(this) {
            PRIMARY -> {
                ButtonDefaults.buttonColors()
            }

            SECONDARY -> {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                )
            }

            DANGER -> {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.red,
                    contentColor = Color.White,
                )
            }
        }
    }


    @Composable
    fun getBorder(): BorderStroke? {
        return when(this) {
            PRIMARY -> {
                null
            }

            SECONDARY -> {
                BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
            }

            DANGER -> {
                null
            }
        }
    }
}