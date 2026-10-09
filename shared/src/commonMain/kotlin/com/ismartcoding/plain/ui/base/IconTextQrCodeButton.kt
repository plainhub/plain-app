package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.qr_code as ui_drawable_qr_code
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextQrCodeButton(onClick: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_qr_code, text = stringResource(Res.string.qrcode), onClick = onClick)
}
