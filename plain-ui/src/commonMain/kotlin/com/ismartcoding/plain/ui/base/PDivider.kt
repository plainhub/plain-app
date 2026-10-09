package com.ismartcoding.plain.ui.base

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PDivider(
    modifier: Modifier = Modifier,
    onContainerColor: Color? = null,
    thickness: Dp = 1.dp,
) {
    val color = onContainerColor?.copy(alpha = 0.12f)
        ?: MaterialTheme.colorScheme.outlineVariant
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = color,
    )
}
