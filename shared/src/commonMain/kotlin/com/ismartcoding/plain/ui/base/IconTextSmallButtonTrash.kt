package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.trash_2 as ui_drawable_trash_2
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextSmallButtonTrash(onClick: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_trash_2, text = stringResource(Res.string.trash), onClick = onClick)
}
