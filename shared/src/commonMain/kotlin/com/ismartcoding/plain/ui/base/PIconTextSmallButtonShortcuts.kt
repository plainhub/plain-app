package com.ismartcoding.plain.ui.base

import com.ismartcoding.plain.i18n.*
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.archive_restore as ui_drawable_archive_restore
import com.ismartcoding.plain.ui.resources.copy as ui_drawable_copy
import com.ismartcoding.plain.ui.resources.delete_forever as ui_drawable_delete_forever
import com.ismartcoding.plain.ui.resources.label as ui_drawable_label
import com.ismartcoding.plain.ui.resources.label_off as ui_drawable_label_off
import com.ismartcoding.plain.ui.resources.link as ui_drawable_link
import com.ismartcoding.plain.ui.resources.package2 as ui_drawable_package2
import com.ismartcoding.plain.ui.resources.package_open as ui_drawable_package_open
import com.ismartcoding.plain.ui.resources.pen as ui_drawable_pen
import com.ismartcoding.plain.ui.resources.playlist_add as ui_drawable_playlist_add
import com.ismartcoding.plain.ui.resources.scissors as ui_drawable_scissors
import com.ismartcoding.plain.ui.resources.share_2 as ui_drawable_share_2
import com.ismartcoding.plain.ui.resources.trash_2 as ui_drawable_trash_2
import com.ismartcoding.plain.i18n.link
import com.ismartcoding.plain.i18n.copy

@Composable
fun IconTextSmallButtonShare(click: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_share_2, text = stringResource(Res.string.share), click = click)
}

@Composable
fun IconTextSmallButtonShareLink(click: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_link, text = stringResource(Res.string.share_link), click = click)
}

@Composable
fun IconTextSmallButtonLabel(click: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_label, text = stringResource(Res.string.add_to_tags), click = click)
}

@Composable
fun IconTextSmallButtonLabelOff(click: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_label_off, text = stringResource(Res.string.remove_from_tags), click = click)
}

@Composable
fun IconTextSmallButtonDelete(click: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_delete_forever, text = stringResource(Res.string.delete), click = click)
}

@Composable
fun IconTextSmallButtonRename(click: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_pen, text = stringResource(Res.string.rename), click = click)
}

@Composable
fun IconTextSmallButtonCut(click: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_scissors, text = stringResource(Res.string.cut), click = click)
}

@Composable
fun IconTextSmallButtonCopy(click: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_copy, text = stringResource(Res.string.copy), click = click)
}

@Composable
fun IconTextSmallButtonQueueAdd(click: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_playlist_add, text = stringResource(Res.string.add_to_queue), click = click)
}

@Composable
fun IconTextSmallButtonRestore(click: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_archive_restore, text = stringResource(Res.string.restore), click = click)
}

@Composable
fun IconTextSmallButtonTrash(click: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_trash_2, text = stringResource(Res.string.trash), click = click)
}

@Composable
fun IconTrashButton(click: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_trash_2, text = stringResource(Res.string.move_to_trash), click = click)
}

@Composable
fun IconTextSmallButtonZip(click: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_package2, text = stringResource(Res.string.compress), click = click)
}

@Composable
fun IconTextSmallButtonUnzip(click: () -> Unit) {
    PIconTextSmallButton(UiRes.drawable.ui_drawable_package_open, text = stringResource(Res.string.decompress), click = click)
}

