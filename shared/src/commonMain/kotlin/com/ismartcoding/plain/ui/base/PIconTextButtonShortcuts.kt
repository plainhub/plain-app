package com.ismartcoding.plain.ui.base

import com.ismartcoding.plain.i18n.*
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextSelectButton(click: () -> Unit) {
    PIconTextActionButton(Res.drawable.list_checks, text = stringResource(Res.string.select), click = click)
}

@Composable
fun IconTextDeleteButton(click: () -> Unit) {
    PIconTextActionButton(Res.drawable.delete_forever, text = stringResource(Res.string.delete), click = click)
}

@Composable
fun IconTextTrashButton(click: () -> Unit) {
    PIconTextActionButton(Res.drawable.trash_2, text = stringResource(Res.string.trash), click = click)
}

@Composable
fun IconTextShareButton(click: () -> Unit) {
    PIconTextActionButton(Res.drawable.share_2, text = stringResource(Res.string.share), click = click)
}

@Composable
fun IconTextOpenWithButton(click: () -> Unit) {
    PIconTextActionButton(Res.drawable.square_arrow_out_up_right, text = stringResource(Res.string.open_with), click = click)
}

@Composable
fun IconTextScanQrCodeButton(click: () -> Unit) {
    PIconTextActionButton(Res.drawable.scan_qr_code, text = stringResource(Res.string.scan_qrcode), click = click)
}

@Composable
fun IconTextRenameButton(click: () -> Unit) {
    PIconTextActionButton(Res.drawable.pen, text = stringResource(Res.string.rename), click = click)
}

@Composable
fun IconTextEditButton(click: () -> Unit) {
    PIconTextActionButton(Res.drawable.square_pen, text = stringResource(Res.string.edit), click = click)
}

@Composable
fun IconTextRestoreButton(click: () -> Unit) {
    PIconTextActionButton(Res.drawable.archive_restore, text = stringResource(Res.string.restore), click = click)
}

@Composable
fun IconTextToTopButton(click: () -> Unit) {
    PIconTextActionButton(Res.drawable.arrow_up_to_line, text = stringResource(Res.string.jump_to_top), click = click)
}

@Composable
fun IconTextToBottomButton(click: () -> Unit) {
    PIconTextActionButton(Res.drawable.arrow_down_to_line, text = stringResource(Res.string.jump_to_bottom), click = click)
}

@Composable
fun IconTextCastButton(click: () -> Unit) {
    PIconTextActionButton(Res.drawable.cast, text = stringResource(Res.string.cast), click = click)
}

@Composable
fun IconTextCutButton(click: () -> Unit) {
    PIconTextActionButton(Res.drawable.scissors, text = stringResource(Res.string.cut), click = click)
}

@Composable
fun IconTextCopyButton(click: () -> Unit) {
    PIconTextActionButton(Res.drawable.copy, text = stringResource(Res.string.copy), click = click)
}

@Composable
fun IconTextZipButton(click: () -> Unit) {
    PIconTextActionButton(Res.drawable.package2, text = stringResource(Res.string.compress), click = click)
}

@Composable
fun IconTextAddToHomeButton(click: () -> Unit) {
    PIconTextActionButton(Res.drawable.smartphone, text = stringResource(Res.string.add_to_home), click = click)
}
