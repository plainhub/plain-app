package com.ismartcoding.plain.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun SwipeActionButton(text: String, color: Color, onClick: () -> Unit) {
    TextButton(
        onClick
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(color = color)
        )
    }
}