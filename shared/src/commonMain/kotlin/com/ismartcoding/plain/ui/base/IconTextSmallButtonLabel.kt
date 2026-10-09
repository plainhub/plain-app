package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.label as ui_drawable_label
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextSmallButtonLabel(onClick: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_label, text = stringResource(Res.string.add_to_tags), onClick = onClick)
}
