package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

@Composable
fun PPlaceholder(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
) {
    Box(modifier.background(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = shape))
}
