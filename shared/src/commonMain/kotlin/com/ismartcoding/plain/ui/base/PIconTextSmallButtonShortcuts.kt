package com.ismartcoding.plain.ui.base

import com.ismartcoding.plain.i18n.*
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextSmallButtonShare(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.share_2, text = stringResource(Res.string.share), click = click)
}

@Composable
fun IconTextSmallButtonLabel(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.label, text = stringResource(Res.string.add_to_tags), click = click)
}

@Composable
fun IconTextSmallButtonLabelOff(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.label_off, text = stringResource(Res.string.remove_from_tags), click = click)
}

@Composable
fun IconTextSmallButtonDelete(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.delete_forever, text = stringResource(Res.string.delete), click = click)
}

@Composable
fun IconTextSmallButtonRename(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.pen, text = stringResource(Res.string.rename), click = click)
}

@Composable
fun IconTextSmallButtonCut(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.scissors, text = stringResource(Res.string.cut), click = click)
}

@Composable
fun IconTextSmallButtonCopy(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.copy, text = stringResource(Res.string.copy), click = click)
}

@Composable
fun IconTextSmallButtonPlaylistAdd(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.playlist_add, text = stringResource(Res.string.add_to_playlist), click = click)
}

@Composable
fun IconTextSmallButtonRestore(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.archive_restore, text = stringResource(Res.string.restore), click = click)
}

@Composable
fun IconTextSmallButtonTrash(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.trash_2, text = stringResource(Res.string.trash), click = click)
}

@Composable
fun IconTrashButton(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.trash_2, text = stringResource(Res.string.move_to_trash), click = click)
}

@Composable
fun IconTextSmallButtonZip(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.package2, text = stringResource(Res.string.compress), click = click)
}

@Composable
fun IconTextSmallButtonUnzip(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.package_open, text = stringResource(Res.string.decompress), click = click)
}
