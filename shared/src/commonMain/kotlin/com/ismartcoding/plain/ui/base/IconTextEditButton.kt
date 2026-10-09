package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.square_pen as ui_drawable_square_pen
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextEditButton(onClick: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_square_pen, text = stringResource(Res.string.edit), onClick = onClick)
}
