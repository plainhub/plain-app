package com.ismartcoding.plain.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.IData
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.base.PSheetPrimaryAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryDeleteAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryTrashAction
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.components.IconTextSmallButtonDelete
import com.ismartcoding.plain.ui.components.IconTextSmallButtonRestore
import com.ismartcoding.plain.ui.components.IconTextSmallButtonTrash
import com.ismartcoding.plain.ui.components.confirmActionAsync
import com.ismartcoding.plain.ui.media.viewer.PreviewItem
import com.ismartcoding.plain.ui.media.viewer.PreviewTrashActions
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.archive_restore as ui_drawable_archive_restore
import com.ismartcoding.plain.ui.tags.TagsViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private fun isSystemPhoto(id: String) = id.startsWith("photos:")

private fun deleteLabel(systemPhotoOnly: Boolean) =
    if (systemPhotoOnly) Res.string.delete_from_system_photos else Res.string.delete

private suspend fun deleteMedia(systemPhotoOnly: Boolean, onDelete: () -> Unit) {
    if (systemPhotoOnly) onDelete()
    else confirmActionAsync(Res.string.delete, Res.string.confirm_to_delete, callback = onDelete, danger = true)
}

@Composable
internal fun MediaTrashSheetActions(
    id: String,
    source: ContentSource,
    trashed: Boolean,
    onTrash: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    if (!trashed) {
        PSheetPrimaryTrashAction(stringResource(Res.string.trash), onClick = onTrash)
        return
    }
    PSheetPrimaryAction(UiRes.drawable.ui_drawable_archive_restore, stringResource(Res.string.restore), onClick = onRestore)
    val systemPhoto = isSystemPhoto(id)
    if (systemPhoto && source is ContentSource.Remote) return
    val scope = rememberCoroutineScope()
    PSheetPrimaryDeleteAction(stringResource(deleteLabel(systemPhoto))) {
        scope.launch { deleteMedia(systemPhoto, onDelete) }
    }
}

@Composable
internal fun <T : IData> MediaSelectionTrashActions(
    vm: BaseMediaViewModel<T>,
    tagsVM: TagsViewModel,
    selection: DragSelectState,
    trashed: Boolean,
) {
    val ids = selection.selectedIds.toSet()
    if (!trashed) {
        IconTextSmallButtonTrash {
            vm.trash(tagsVM, ids)
            selection.exitSelectMode()
        }
        return
    }
    IconTextSmallButtonRestore {
        vm.restore(tagsVM, ids)
        selection.exitSelectMode()
    }
    if (vm.source is ContentSource.Remote && ids.any(::isSystemPhoto)) return
    val scope = rememberCoroutineScope()
    val systemPhotoOnly = ids.isNotEmpty() && ids.all(::isSystemPhoto)
    IconTextSmallButtonDelete(deleteLabel(systemPhotoOnly)) {
        scope.launch {
            deleteMedia(systemPhotoOnly) {
                vm.delete(tagsVM, ids)
                selection.exitSelectMode()
            }
        }
    }
}

internal fun <T : IData> BaseMediaViewModel<T>.previewTrashActions(
    tagsVM: TagsViewModel,
    onChanged: () -> Unit,
): PreviewTrashActions {
    fun apply(item: PreviewItem, action: (TagsViewModel, Set<String>) -> Unit) {
        action(tagsVM, setOf(item.mediaId))
        onChanged()
    }
    return PreviewTrashActions(
        trashed = trash.value,
        trash = { apply(it, ::trash) },
        restore = { apply(it, ::restore) },
        delete = { apply(it, ::delete) },
    )
}
