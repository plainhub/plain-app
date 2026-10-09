package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.label_off as ui_drawable_label_off
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextSmallButtonLabelOff(onClick: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_label_off, text = stringResource(Res.string.remove_from_tags), onClick = onClick)
}
