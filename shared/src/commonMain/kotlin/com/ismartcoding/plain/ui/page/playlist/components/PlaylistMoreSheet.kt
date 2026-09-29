package com.ismartcoding.plain.ui.page.playlist.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.helpers.DialogHelper
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.delete_forever as ui_drawable_delete_forever
import com.ismartcoding.plain.ui.resources.pen as ui_drawable_pen
import com.ismartcoding.plain.ui.resources.plus as ui_drawable_plus

/** Manage menu: rename, add items, delete (with confirm). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistMoreSheet(
    playlistName: String,
    itemCount: Int,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onAddItems: () -> Unit,
    onDelete: () -> Unit,
) {
    PModalBottomSheet(onDismissRequest = onDismiss) {
        PSheetActionRow(UiRes.drawable.ui_drawable_pen, stringResource(Res.string.rename_playlist)) {
            onDismiss()
            onRename()
        }
        PSheetActionRow(UiRes.drawable.ui_drawable_plus, stringResource(Res.string.add_items)) {
            onDismiss()
            onAddItems()
        }
        PSheetActionRow(UiRes.drawable.ui_drawable_delete_forever, stringResource(Res.string.delete_playlist)) {
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
