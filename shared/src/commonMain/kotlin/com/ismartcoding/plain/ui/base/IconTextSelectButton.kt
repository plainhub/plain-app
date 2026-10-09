package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.list_checks as ui_drawable_list_checks
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextSelectButton(onClick: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_list_checks, text = stringResource(Res.string.select), onClick = onClick)
}
