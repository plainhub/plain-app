package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.pen as ui_drawable_pen
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextRenameButton(onClick: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_pen, text = stringResource(Res.string.rename), onClick = onClick)
}
