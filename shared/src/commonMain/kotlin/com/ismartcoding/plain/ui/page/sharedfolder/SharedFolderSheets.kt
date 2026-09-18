package com.ismartcoding.plain.ui.page.sharedfolder

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.features.share.SharedFileDto
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.getDownloadsDirPath
import com.ismartcoding.plain.ui.components.SaveToSheet
import org.jetbrains.compose.resources.stringResource

/** Save-as sheet for one entry: folders mirror or zip, files stream. */
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
            if (target.isDir) state.syncDirTo(target, "${getDownloadsDirPath().trimEnd('/')}/PlainApp")
            else state.downloadFileToDownloads(target)
        },
        onDirectory = { dir ->
            state.downloadTarget = null
            if (target.isDir) state.syncDirTo(target, dir) else state.downloadFileToDir(target, dir)
        },
        onZip = target.takeIf { it.isDir }?.let { entry ->
            {
                state.downloadTarget = null
                state.downloadZipToDownloads(entry)
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
            state.saveSelectionToDownloads()
        },
        onDirectory = { dir ->
            state.showSaveSelectedSheet = false
            state.saveSelectionToDir(dir)
        },
        onZip = {
            state.showSaveSelectedSheet = false
            state.zipSelection()
        },
    )
}
