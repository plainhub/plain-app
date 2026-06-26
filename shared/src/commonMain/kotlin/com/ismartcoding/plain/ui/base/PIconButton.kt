package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource

@Composable
expect fun PIconButton(
    icon: DrawableResource,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    iconSize: Dp = 24.dp,
    tint: Color = androidx.compose.material3.LocalContentColor.current,
    contentDescription: String? = null,
    showBadge: Boolean = false,
    isHaptic: Boolean? = false,
    isSound: Boolean? = false,
    enabled: Boolean = true,
    click: () -> Unit = {},
)
