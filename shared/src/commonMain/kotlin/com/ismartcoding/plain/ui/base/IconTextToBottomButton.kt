package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.arrow_down_to_line as ui_drawable_arrow_down_to_line
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextToBottomButton(onClick: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_arrow_down_to_line, text = stringResource(Res.string.jump_to_bottom), onClick = onClick)
}
