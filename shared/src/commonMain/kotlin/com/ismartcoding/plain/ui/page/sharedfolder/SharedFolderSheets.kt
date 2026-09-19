package com.ismartcoding.plain.ui.page.sharedfolder

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.features.share.SharedFileDto
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.components.SaveToSheet
import org.jetbrains.compose.resources.stringResource

/**
 * Save-as sheet for one entry, reusing the shared [SaveToSheet]: folders
 * mirror (or zip), files stream. The chosen destination enqueues a batch.
 */
@Composable
internal fun SharedFolderDownloadSheet(
    state: SharedFolderState,
    target: SharedFileDto,
) {
    SaveToSheet(
        title = target.name,
        onDismiss = { state.downloadTarget = null },
        onDownloads = {
            state.downloadTarget = null
            state.enqueueEntry(target, "")
        },
        onDirectory = { dir ->
            state.downloadTarget = null
            state.enqueueEntry(target, dir)
        },
        onZip = target.takeIf { it.isDir }?.let { entry ->
            {
                state.downloadTarget = null
                state.enqueueEntryZip(entry)
            }
        },
    )
}

/** Save-as sheet for the current multi-selection. */
@Composable
internal fun SharedFolderSaveSelectionSheet(state: SharedFolderState) {
    SaveToSheet(
        title = stringResource(Res.string.save_selected, state.selected.size),
        onDismiss = { state.showSaveSelectedSheet = false },
        onDownloads = {
            state.showSaveSelectedSheet = false
            state.enqueueSelection("")
        },
        onDirectory = { dir ->
            state.showSaveSelectedSheet = false
            state.enqueueSelection(dir)
        },
        onZip = {
            state.showSaveSelectedSheet = false
            state.enqueueSelectionZip()
        },
    )
}
