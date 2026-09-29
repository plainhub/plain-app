package com.ismartcoding.plain.ui.base

import com.ismartcoding.plain.i18n.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.arrow_left as ui_drawable_arrow_left
import com.ismartcoding.plain.ui.resources.x as ui_drawable_x

@Composable
fun NavigationBackIcon(onClick: () -> Unit = {}) {
    PIconButton(
        icon = UiRes.drawable.ui_drawable_arrow_left,
        contentDescription = stringResource(Res.string.back),
        tint = MaterialTheme.colorScheme.onSurface,
    ) {
        onClick()
    }
}

@Composable
fun NavigationCloseIcon(onClick: () -> Unit = {}) {
    PIconButton(
        icon = UiRes.drawable.ui_drawable_x,
        contentDescription = stringResource(Res.string.close),
        tint = MaterialTheme.colorScheme.onSurface,
    ) {
        onClick()
    }
}
