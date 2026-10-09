package com.ismartcoding.plain.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.i18n.sort
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.sort as ui_drawable_sort
import org.jetbrains.compose.resources.stringResource

@Composable
fun ActionButtonSort(onClick: () -> Unit) {
    PIconButton(
        icon = UiRes.drawable.ui_drawable_sort,
        contentDescription = stringResource(Res.string.sort),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}
