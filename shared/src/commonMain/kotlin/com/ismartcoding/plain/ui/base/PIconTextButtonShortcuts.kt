package com.ismartcoding.plain.ui.base

import com.ismartcoding.plain.i18n.*
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.archive_restore as ui_drawable_archive_restore
import com.ismartcoding.plain.ui.resources.arrow_down_to_line as ui_drawable_arrow_down_to_line
import com.ismartcoding.plain.ui.resources.arrow_up_to_line as ui_drawable_arrow_up_to_line
import com.ismartcoding.plain.ui.resources.cast as ui_drawable_cast
import com.ismartcoding.plain.ui.resources.copy as ui_drawable_copy
import com.ismartcoding.plain.ui.resources.delete_forever as ui_drawable_delete_forever
import com.ismartcoding.plain.ui.resources.forward as ui_drawable_forward
import com.ismartcoding.plain.ui.resources.link as ui_drawable_link
import com.ismartcoding.plain.ui.resources.list_checks as ui_drawable_list_checks
import com.ismartcoding.plain.ui.resources.package2 as ui_drawable_package2
import com.ismartcoding.plain.ui.resources.pen as ui_drawable_pen
import com.ismartcoding.plain.ui.resources.qr_code as ui_drawable_qr_code
import com.ismartcoding.plain.ui.resources.scan_qr_code as ui_drawable_scan_qr_code
import com.ismartcoding.plain.ui.resources.scissors as ui_drawable_scissors
import com.ismartcoding.plain.ui.resources.share_2 as ui_drawable_share_2
import com.ismartcoding.plain.ui.resources.square_arrow_out_up_right as ui_drawable_square_arrow_out_up_right
import com.ismartcoding.plain.ui.resources.square_pen as ui_drawable_square_pen
import com.ismartcoding.plain.ui.resources.trash_2 as ui_drawable_trash_2
import com.ismartcoding.plain.i18n.forward
import com.ismartcoding.plain.i18n.link
import com.ismartcoding.plain.i18n.copy
import com.ismartcoding.plain.i18n.cast

@Composable
fun IconTextSelectButton(click: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_list_checks, text = stringResource(Res.string.select), click = click)
}

@Composable
fun IconTextDeleteButton(click: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_delete_forever, text = stringResource(Res.string.delete), click = click)
}

@Composable
fun IconTextTrashButton(click: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_trash_2, text = stringResource(Res.string.trash), click = click)
}

@Composable
fun IconTextShareButton(click: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_share_2, text = stringResource(Res.string.share), click = click)
}

@Composable
fun IconTextShareLinkButton(click: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_link, text = stringResource(Res.string.share_link), click = click)
}

@Composable
fun IconTextOpenWithButton(click: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_square_arrow_out_up_right, text = stringResource(Res.string.open_with), click = click)
}

@Composable
fun IconTextScanQrCodeButton(click: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_scan_qr_code, text = stringResource(Res.string.scan_qrcode), click = click)
}

@Composable
fun IconTextRenameButton(click: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_pen, text = stringResource(Res.string.rename), click = click)
}

@Composable
fun IconTextEditButton(click: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_square_pen, text = stringResource(Res.string.edit), click = click)
}

@Composable
fun IconTextRestoreButton(click: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_archive_restore, text = stringResource(Res.string.restore), click = click)
}

@Composable
fun IconTextToTopButton(click: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_arrow_up_to_line, text = stringResource(Res.string.jump_to_top), click = click)
}

@Composable
fun IconTextToBottomButton(click: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_arrow_down_to_line, text = stringResource(Res.string.jump_to_bottom), click = click)
}

@Composable
fun IconTextCastButton(click: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_cast, text = stringResource(Res.string.cast), click = click)
}

@Composable
fun IconTextCutButton(click: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_scissors, text = stringResource(Res.string.cut), click = click)
}

@Composable
fun IconTextCopyButton(click: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_copy, text = stringResource(Res.string.copy), click = click)
}

@Composable
fun IconTextZipButton(click: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_package2, text = stringResource(Res.string.compress), click = click)
}

@Composable
fun IconTextForwardButton(click: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_forward, text = stringResource(Res.string.forward), click = click)
}

@Composable
fun IconTextQrCodeButton(click: () -> Unit) {
    PIconTextActionButton(UiRes.drawable.ui_drawable_qr_code, text = stringResource(Res.string.qrcode), click = click)
}