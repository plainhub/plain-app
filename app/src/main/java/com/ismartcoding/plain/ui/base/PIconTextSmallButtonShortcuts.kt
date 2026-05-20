package com.ismartcoding.plain.ui.base

import com.ismartcoding.plain.i18n.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R

@Composable
fun IconTextSmallButtonShare(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.share_2, text = stringResource(R.string.share), click = click)
}

@Composable
fun IconTextSmallButtonLabel(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.label, text = stringResource(R.string.add_to_tags), click = click)
}

@Composable
fun IconTextSmallButtonLabelOff(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.label_off, text = stringResource(R.string.remove_from_tags), click = click)
}

@Composable
fun IconTextSmallButtonDelete(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.delete_forever, text = stringResource(R.string.delete), click = click)
}

@Composable
fun IconTextSmallButtonRename(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.pen, text = stringResource(R.string.rename), click = click)
}

@Composable
fun IconTextSmallButtonCut(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.scissors, text = stringResource(R.string.cut), click = click)
}

@Composable
fun IconTextSmallButtonCopy(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.copy, text = stringResource(R.string.copy), click = click)
}

@Composable
fun IconTextSmallButtonPlaylistAdd(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.playlist_add, text = stringResource(R.string.add_to_playlist), click = click)
}

@Composable
fun IconTextSmallButtonRestore(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.archive_restore, text = stringResource(R.string.restore), click = click)
}

@Composable
fun IconTextSmallButtonTrash(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.trash_2, text = stringResource(R.string.trash), click = click)
}

@Composable
fun IconTrashButton(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.trash_2, text = stringResource(R.string.move_to_trash), click = click)
}

@Composable
fun IconTextSmallButtonZip(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.package2, text = stringResource(R.string.compress), click = click)
}

@Composable
fun IconTextSmallButtonUnzip(click: () -> Unit) {
    PIconTextSmallButton(Res.drawable.package_open, text = stringResource(R.string.decompress), click = click)
}
