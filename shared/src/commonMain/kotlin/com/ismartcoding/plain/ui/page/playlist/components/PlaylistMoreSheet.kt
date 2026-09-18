package com.ismartcoding.plain.ui.page.playlist.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.page.playlist.PlaylistSortOrder
import org.jetbrains.compose.resources.stringResource

/** Manage menu: rename, add items, cycle sort order, delete (with confirm). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistMoreSheet(
    playlistName: String,
    itemCount: Int,
    sort: PlaylistSortOrder,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onAddItems: () -> Unit,
    onToggleSort: () -> Unit,
    onDelete: () -> Unit,
) {
    PModalBottomSheet(onDismissRequest = onDismiss) {
        PBottomSheetTopAppBar(title = playlistName)
        PSheetActionRow(Res.drawable.pen, stringResource(Res.string.rename_playlist)) {
            onDismiss()
            onRename()
        }
        PSheetActionRow(Res.drawable.plus, stringResource(Res.string.add_items)) {
            onDismiss()
            onAddItems()
        }
        PSheetActionRow(Res.drawable.sort, stringResource(Res.string.sort_order) + " · " + sort.label()) {
            onDismiss()
            onToggleSort()
        }
        PSheetActionRow(Res.drawable.delete_forever, stringResource(Res.string.delete_playlist)) {
            onDismiss()
            DialogHelper.showConfirmDialog(
                title = LocaleHelper.getString(Res.string.delete_playlist),
                message = LocaleHelper.getStringF(
                    Res.string.delete_playlist_confirm_text,
                    playlistName,
                    itemCount.toString(),
                ),
                confirmButton = Pair(LocaleHelper.getString(Res.string.delete)) { onDelete() },
                dismissButton = Pair(LocaleHelper.getString(Res.string.cancel)) {},
                danger = true,
            )
        }
        BottomSpace()
    }
}
