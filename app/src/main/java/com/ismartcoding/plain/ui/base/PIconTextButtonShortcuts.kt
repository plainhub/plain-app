package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R

@Composable
fun IconTextSelectButton(click: () -> Unit) {
    PIconTextActionButton(R.drawable.list_checks, text = stringResource(R.string.select), click = click)
}

@Composable
fun IconTextDeleteButton(click: () -> Unit) {
    PIconTextActionButton(R.drawable.delete_forever, text = stringResource(R.string.delete), click = click)
}

@Composable
fun IconTextTrashButton(click: () -> Unit) {
    PIconTextActionButton(R.drawable.trash_2, text = stringResource(R.string.trash), click = click)
}

@Composable
fun IconTextShareButton(click: () -> Unit) {
    PIconTextActionButton(R.drawable.share_2, text = stringResource(R.string.share), click = click)
}

@Composable
fun IconTextOpenWithButton(click: () -> Unit) {
    PIconTextActionButton(R.drawable.square_arrow_out_up_right, text = stringResource(R.string.open_with), click = click)
}

@Composable
fun IconTextScanQrCodeButton(click: () -> Unit) {
    PIconTextActionButton(R.drawable.scan_qr_code, text = stringResource(R.string.scan_qrcode), click = click)
}

@Composable
fun IconTextRenameButton(click: () -> Unit) {
    PIconTextActionButton(R.drawable.pen, text = stringResource(R.string.rename), click = click)
}

@Composable
fun IconTextEditButton(click: () -> Unit) {
    PIconTextActionButton(R.drawable.square_pen, text = stringResource(R.string.edit), click = click)
}

@Composable
fun IconTextRestoreButton(click: () -> Unit) {
    PIconTextActionButton(R.drawable.archive_restore, text = stringResource(R.string.restore), click = click)
}

@Composable
fun IconTextToTopButton(click: () -> Unit) {
    PIconTextActionButton(R.drawable.arrow_up_to_line, text = stringResource(R.string.jump_to_top), click = click)
}

@Composable
fun IconTextToBottomButton(click: () -> Unit) {
    PIconTextActionButton(R.drawable.arrow_down_to_line, text = stringResource(R.string.jump_to_bottom), click = click)
}

@Composable
fun IconTextCastButton(click: () -> Unit) {
    PIconTextActionButton(R.drawable.cast, text = stringResource(R.string.cast), click = click)
}
