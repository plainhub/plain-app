package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.package2 as ui_drawable_package2
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextSmallButtonZip(onClick: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_package2, text = stringResource(Res.string.compress), onClick = onClick)
}
