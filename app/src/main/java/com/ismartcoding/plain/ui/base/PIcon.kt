package com.ismartcoding.plain.ui.base

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PIcon(
    modifier: Modifier = Modifier,
    icon: Any,
    contentDescription: String?,
    tint: Color = LocalContentColor.current,
) {
    if (icon is ImageVector) {
        Icon(
            modifier = modifier,
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
        )
    } else if (icon is Painter) {
        Icon(
            modifier = modifier,
            painter = icon,
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}
