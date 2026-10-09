package com.ismartcoding.plain.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.info as ui_drawable_info

@Composable
fun ActionButtonInfo(contentDescription: String, onClick: () -> Unit) {
    PIconButton(
        icon = UiRes.drawable.ui_drawable_info,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}
