package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R

@Composable
fun IconTextSmallButtonShare(click: () -> Unit) {
    PIconTextSmallButton(R.drawable.share_2, text = stringResource(R.string.share), click = click)
}

@Composable
fun IconTextSmallButtonLabel(click: () -> Unit) {
    PIconTextSmallButton(R.drawable.label, text = stringResource(R.string.add_to_tags), click = click)
}

@Composable
fun IconTextSmallButtonLabelOff(click: () -> Unit) {
    PIconTextSmallButton(R.drawable.label_off, text = stringResource(R.string.remove_from_tags), click = click)
}

@Composable
fun IconTextSmallButtonDelete(click: () -> Unit) {
    PIconTextSmallButton(R.drawable.delete_forever, text = stringResource(R.string.delete), click = click)
}

@Composable
fun IconTextSmallButtonRename(click: () -> Unit) {
    PIconTextSmallButton(R.drawable.pen, text = stringResource(R.string.rename), click = click)
}

@Composable
fun IconTextSmallButtonCut(click: () -> Unit) {
    PIconTextSmallButton(R.drawable.scissors, text = stringResource(R.string.cut), click = click)
}

@Composable
fun IconTextSmallButtonCopy(click: () -> Unit) {
    PIconTextSmallButton(R.drawable.copy, text = stringResource(R.string.copy), click = click)
}

@Composable
fun IconTextSmallButtonPlaylistAdd(click: () -> Unit) {
    PIconTextSmallButton(R.drawable.playlist_add, text = stringResource(R.string.add_to_playlist), click = click)
}

@Composable
fun IconTextSmallButtonRestore(click: () -> Unit) {
    PIconTextSmallButton(R.drawable.archive_restore, text = stringResource(R.string.restore), click = click)
}

@Composable
fun IconTextSmallButtonTrash(click: () -> Unit) {
    PIconTextSmallButton(R.drawable.trash_2, text = stringResource(R.string.trash), click = click)
}

@Composable
fun IconTrashButton(click: () -> Unit) {
    PIconTextSmallButton(R.drawable.trash_2, text = stringResource(R.string.move_to_trash), click = click)
}

@Composable
fun IconTextSmallButtonZip(click: () -> Unit) {
    PIconTextSmallButton(R.drawable.package2, text = stringResource(R.string.compress), click = click)
}

@Composable
fun IconTextSmallButtonUnzip(click: () -> Unit) {
    PIconTextSmallButton(R.drawable.package_open, text = stringResource(R.string.decompress), click = click)
}
