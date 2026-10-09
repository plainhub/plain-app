package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.package_open as ui_drawable_package_open
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextSmallButtonUnzip(onClick: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_package_open, text = stringResource(Res.string.decompress), onClick = onClick)
}
