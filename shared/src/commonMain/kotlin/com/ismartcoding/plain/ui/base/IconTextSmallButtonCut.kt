package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.scissors as ui_drawable_scissors
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextSmallButtonCut(onClick: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_scissors, text = stringResource(Res.string.cut), onClick = onClick)
}
