package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.scan_qr_code as ui_drawable_scan_qr_code
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextScanQrCodeButton(onClick: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_scan_qr_code, text = stringResource(Res.string.scan_qrcode), onClick = onClick)
}
