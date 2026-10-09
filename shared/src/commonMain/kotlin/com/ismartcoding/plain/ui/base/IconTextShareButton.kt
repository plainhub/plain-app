package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.share_2 as ui_drawable_share_2
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextShareButton(onClick: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_share_2, text = stringResource(Res.string.share), onClick = onClick)
}
