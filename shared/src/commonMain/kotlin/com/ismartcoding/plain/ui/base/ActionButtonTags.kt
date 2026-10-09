package com.ismartcoding.plain.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.tag as ui_drawable_tag
import org.jetbrains.compose.resources.stringResource

@Composable
fun ActionButtonTags(onClick: () -> Unit) {
    PIconButton(
        icon = UiRes.drawable.ui_drawable_tag,
        contentDescription = stringResource(Res.string.tags),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}
