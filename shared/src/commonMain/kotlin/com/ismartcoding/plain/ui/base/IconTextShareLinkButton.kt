package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.link as ui_drawable_link
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextShareLinkButton(onClick: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_link, text = stringResource(Res.string.share_link), onClick = onClick)
}
