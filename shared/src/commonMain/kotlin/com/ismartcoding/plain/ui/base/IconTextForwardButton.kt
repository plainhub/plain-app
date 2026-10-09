package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.i18n.forward
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.forward as ui_drawable_forward
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextForwardButton(onClick: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_forward, text = stringResource(Res.string.forward), onClick = onClick)
}
