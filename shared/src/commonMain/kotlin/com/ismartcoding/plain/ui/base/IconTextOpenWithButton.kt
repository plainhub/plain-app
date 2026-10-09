package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.square_arrow_out_up_right as ui_drawable_square_arrow_out_up_right
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextOpenWithButton(onClick: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_square_arrow_out_up_right, text = stringResource(Res.string.open_with), onClick = onClick)
}
