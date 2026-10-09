package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.i18n.copy
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.copy as ui_drawable_copy
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextCopyButton(onClick: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_copy, text = stringResource(Res.string.copy), onClick = onClick)
}
