package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.archive_restore as ui_drawable_archive_restore
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextRestoreButton(onClick: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_archive_restore, text = stringResource(Res.string.restore), onClick = onClick)
}
