package com.ismartcoding.plain.ui.components.codeeditor

import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import com.ismartcoding.plain.ui.base.PIcon

@Composable
internal fun EditorIconButton(
    icon: DrawableResource,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    enabled: Boolean = true,
    click: () -> Unit,
) {
    IconButton(modifier = modifier, enabled = enabled, onClick = click) {
        PIcon(icon = painterResource(icon), contentDescription = null, tint = tint)
    }
}
