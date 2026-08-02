package com.ismartcoding.plain.ui.base

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 通用分隔线封装。
 *
 * @param onContainerColor 该 divider 所在容器的背景色（用于派生对比色），
 *   传 null 时使用默认 outlineVariant（适用于普通卡片/表面背景）。
 */
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
