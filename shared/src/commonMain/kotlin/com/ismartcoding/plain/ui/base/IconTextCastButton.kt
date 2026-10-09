package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.i18n.cast
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.cast as ui_drawable_cast
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextCastButton(onClick: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_cast, text = stringResource(Res.string.cast), onClick = onClick)
}
