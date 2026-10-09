package com.ismartcoding.plain.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.folder as ui_drawable_folder
import org.jetbrains.compose.resources.stringResource

@Composable
fun ActionButtonFolders(onClick: () -> Unit) {
    PIconButton(
        icon = UiRes.drawable.ui_drawable_folder,
        contentDescription = stringResource(Res.string.folders),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}
