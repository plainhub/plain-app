package com.ismartcoding.plain.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.i18n.search
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.search as ui_drawable_search
import org.jetbrains.compose.resources.stringResource

@Composable
fun ActionButtonSearch(onClick: () -> Unit) {
    PIconButton(
        icon = UiRes.drawable.ui_drawable_search,
        contentDescription = stringResource(Res.string.search),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}
